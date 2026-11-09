#!/usr/bin/env python3
"""Extract the service/operation/DTO model of the ah5-core-java-spring core systems from the Java sources."""
import json, re, sys
from pathlib import Path

ROOT = Path(sys.argv[1])            # .../Arrowhead
CORE = ROOT / "ah5-core-java-spring"
COMMON = ROOT / "ah5-common-java-spring"

SYSTEMS = {
    "ServiceRegistry": ("serviceregistry", "serviceregistry", "ServiceRegistry"),
    "Authentication": ("authentication", "authentication", "Authentication"),
    "ConsumerAuthorization": ("consumer-authorization", "authorization", "Authorization"),
    "DynamicServiceOrchestration": ("serviceorchestration-dynamic", "serviceorchestration", "DynamicServiceOrchestration"),
    "SimpleStoreServiceOrchestration": ("serviceorchestration-simple", "serviceorchestration", "SimpleStoreServiceOrchestration"),
}

def strip_comments(s):
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    return re.sub(r"//[^\n]*", "", s)

def parse_constants(files):
    raw = {}
    for f in files:
        cls = f.stem
        txt = strip_comments(f.read_text())
        for m in re.finditer(r"(?:public|private|protected)?\s*static\s+final\s+(?:String|int|long|boolean)\s+(\$?\w+)\s*=\s*(.*?);", txt, re.S):
            raw[(cls, m.group(1))] = m.group(2).strip()
    return raw

class Resolver:
    def __init__(self, raw):
        self.raw = raw
        self.byname = {}
        for (c, n), v in raw.items():
            self.byname.setdefault(n, []).append((c, v))
    def get(self, expr, ctx_cls=None, depth=0):
        expr = expr.strip()
        if depth > 20:
            return None
        parts = split_plus(expr)
        out = ""
        for p in parts:
            p = p.strip()
            if p.startswith('"'):
                out += bytes(p[1:-1], "utf-8").decode("unicode_escape")
                continue
            if "." in p:
                c, n = p.rsplit(".", 1)
                c = c.split(".")[-1]
                v = self.raw.get((c, n))
                if v is None:
                    return None
                r = self.get(v, c, depth + 1)
            else:
                v = self.raw.get((ctx_cls, p)) if ctx_cls else None
                if v is None:
                    cands = self.byname.get(p, [])
                    if not cands:
                        return None
                    c, v = cands[0]
                else:
                    c = ctx_cls
                r = self.get(v, c, depth + 1)
            if r is None:
                return None
            out += r
        return out

def split_plus(expr):
    parts, cur, q = [], "", False
    for ch in expr:
        if ch == '"':
            q = not q
        if ch == "+" and not q:
            parts.append(cur); cur = ""
        else:
            cur += ch
    parts.append(cur)
    return parts

def java_files(base):
    return list(base.rglob("*.java"))

def balanced(txt, start, open_ch="(", close_ch=")"):
    depth = 0
    for i in range(start, len(txt)):
        if txt[i] == open_ch:
            depth += 1
        elif txt[i] == close_ch:
            depth -= 1
            if depth == 0:
                return i
    return -1

def parse_annotation_arg(ann_args, key):
    m = re.search(key + r"\s*=\s*([^,)]+(?:\+[^,)]+)*)", ann_args)
    return m.group(1).strip() if m else None

def parse_api(f, res):
    txt = strip_comments(f.read_text())
    cls = f.stem
    m = re.search(r"@RequestMapping\(([^)]*)\)\s*(?:@\w+(?:\([^)]*\))?\s*)*public\s+class", txt)
    base = res.get(m.group(1).replace("path =", "").replace("value =", "").strip(), cls) if m else ""
    eps = []
    for mm in re.finditer(r"@(Get|Post|Put|Delete|Patch)Mapping\b", txt):
        method = mm.group(1).upper()
        i = mm.end()
        args = ""
        if i < len(txt) and txt[i] == "(":
            j = balanced(txt, i)
            args = txt[i + 1:j]
            i = j + 1
        path_expr = parse_annotation_arg(args, "path") or parse_annotation_arg(args, "value")
        if path_expr is None and args.strip() and "=" not in args:
            path_expr = args.strip()
        path = res.get(path_expr, cls) if path_expr else ""
        # look back for @Operation summary and @ResponseStatus
        prev = txt[max(0, mm.start() - 6000):mm.start()]
        lastop = prev.rfind("@Operation")
        summary = None
        if lastop >= 0:
            sm = re.search(r'summary\s*=\s*"([^"]*)"', prev[lastop:])
            summary = sm.group(1) if sm else None
        codes = re.findall(r"responseCode\s*=\s*Constants\.HTTP_STATUS_(\w+)", prev[lastop:] if lastop >= 0 else "")
        sig_start = i
        # the method body starts at the first "{" outside string literals and parentheses
        depth, q, brace = 0, False, len(txt)
        for k in range(sig_start, len(txt)):
            ch = txt[k]
            if ch == '"' and txt[k - 1] != "\\":
                q = not q
            elif not q:
                if ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                elif ch == "{" and depth == 0:
                    brace = k
                    break
        sig = txt[sig_start:brace]
        rs = re.search(r"@ResponseStatus\((?:value\s*=\s*)?HttpStatus\.(\w+)\)", sig)
        sm2 = re.search(r"public\s+(?:@ResponseBody\s+)?([\w<>, ?\[\]]+?)\s+(\w+)\s*\(", sig)
        ret, name = (sm2.group(1).strip(), sm2.group(2)) if sm2 else (None, None)
        params_txt = sig[sig.find("(", sm2.end() - 1) + 1: sig.rfind(")")] if sm2 else ""
        while "@io.swagger" in params_txt:
            k = params_txt.find("@io.swagger")
            p0 = params_txt.find("(", k)
            end = balanced(params_txt, p0)
            if end < 0:
                break
            params_txt = params_txt[:k] + params_txt[end + 1:]
        body = re.search(r"@RequestBody(?:\([^)]*\))?\s+(?:final\s+)?([\w<>, ?]+?)\s+\w+", params_txt)
        qps = []
        for q in re.finditer(r"@RequestParam(\([^)]*\))?\s+(?:final\s+)?([\w<>]+)\s+(\w+)", params_txt):
            qa = q.group(1) or ""
            req = "required = false" not in qa and "defaultValue" not in qa
            dv = re.search(r"defaultValue\s*=\s*([^,)]+)", qa)
            nm = re.search(r'(?:name|value)\s*=\s*([^,)]+)', qa)
            pname = q.group(3)
            if nm:
                r = res.get(nm.group(1), cls)
                pname = r if r else pname
            qps.append({"name": pname, "type": q.group(2), "required": req,
                        "default": (res.get(dv.group(1), cls) if dv else None)})
        pvs = [p.group(2) for p in re.finditer(r"@PathVariable(\([^)]*\))?\s+(?:final\s+)?[\w<>]+\s+(\w+)", params_txt)]
        eps.append({"class": cls, "method": method, "path": base + path, "javaMethod": name,
                    "summary": summary, "requestBody": body.group(1).strip() if body else None,
                    "queryParams": qps, "pathVars": pvs, "response": ret,
                    "status": rs.group(1) if rs else None, "codes": codes})
    return eps

def parse_sysinfo(f, res):
    txt = strip_comments(f.read_text())
    cls = f.stem
    funcs = {}
    for m in re.finditer(r"private\s+InterfaceModel\s+(\w+)\s*\(([^)]*)\)\s*\{", txt):
        j = balanced(txt, m.end() - 1, "{", "}")
        funcs[m.group(1)] = (m.group(2), txt[m.end():j])
    def http_ops(fname, basepath_arg=None):
        params, body = funcs[fname]
        if basepath_arg:
            names = [p.strip().split()[-1] for p in params.split(",") if p.strip()]
            args = [a.strip() for a in basepath_arg.split(",")]
            for name, arg in zip(names, args):
                body = re.sub(r"(?<![\w.])" + re.escape(name) + r"\b", lambda _m, arg=arg: arg, body)
        call = re.search(r"return\s+(\w+)\(([^)]*)\);", body)
        if call and call.group(1) in funcs and "Builder" not in body:
            return http_ops(call.group(1), call.group(2))
        vars_ = {}
        for v in re.finditer(r"HttpOperationModel\s+(\w+)\s*=\s*new\s+HttpOperationModel\.Builder\(\)\s*\.method\(HttpMethod\.(\w+)\.name\(\)\)\s*\.path\(([^)]*)\)", body):
            vars_[v.group(1)] = (v.group(2), res.get(v.group(3), cls))
        bp = re.search(r"\.basePath\(([^;]*?)\)\s*\.", body)
        bpe = bp.group(1) if bp else ""
        if bpe.strip() == "basePath" and basepath_arg:
            bpe = basepath_arg
        base = res.get(bpe, cls)
        ops = []
        for o in re.finditer(r'\.operation\(([\w.]+|"[^"]*"),\s*(\w+)\)', body):
            mth, p = vars_[o.group(2)]
            ops.append({"name": res.get(o.group(1), cls), "method": mth, "path": p})
        return {"basePath": base, "ops": ops}
    def mqtt_ops(fname, topic_arg=None):
        params, body = funcs[fname]
        call = re.search(r"return\s+(\w+)\(([^)]*)\);", body)
        if call and call.group(1) in funcs and "Builder" not in body:
            return mqtt_ops(call.group(1), call.group(2))
        bt = re.search(r"\.baseTopic\(([^)]*)\)", body)
        if bt and bt.group(1).strip() == "baseTopic" and topic_arg:
            return {"baseTopic": res.get(topic_arg, cls), "ops": mqtt_ops_list(body)}
        return {"baseTopic": res.get(bt.group(1), cls), "ops": mqtt_ops_list(body)}
    def mqtt_ops_list(body):
        opsm = re.search(r"\.operations\(Set\.of\((.*?)\)\)", body, re.S)
        return [res.get(x.strip(), cls) for x in opsm.group(1).split(",")] if opsm else []
    gs = re.search(r"public\s+List<ServiceModel>\s+getServices\(\)\s*\{", txt)
    gbody = txt[gs.end(): balanced(txt, gs.end() - 1, "{", "}")]
    services = []
    for sm in re.finditer(r"new\s+ServiceModel\.Builder\(\)(.*?)\.build\(\)", gbody, re.S):
        b = sm.group(1)
        sd = res.get(re.search(r"\.serviceDefinition\(([^)]*)\)", b).group(1), cls)
        ver = res.get(re.search(r"\.version\(([^)]*)\)", b).group(1), cls)
        meta = re.findall(r"\.metadata\(([\w.]+),\s*([^)]*)\)", b)
        ifs = re.findall(r"\.serviceInterface\((\w+)\(\)\)", b)
        svc = {"serviceDefinition": sd, "version": ver,
               "metadata": {res.get(k, cls) or k: v.strip() for k, v in meta}}
        for i in ifs:
            if "Http" in i:
                svc["http"] = http_ops(i)
            elif "Mqtt" in i:
                svc["mqtt"] = mqtt_ops(i)
        services.append(svc)
    return services

def parse_mqtt_handlers(d, res):
    out = {}
    for f in d.glob("*.java"):
        txt = strip_comments(f.read_text())
        bt = re.search(r"baseTopic\(\)\s*\{\s*return\s+([^;]+);", txt)
        if not bt:
            continue
        topic = res.get(bt.group(1), f.stem)
        ops = {}
        for c in re.finditer(r"case\s+([\w.]+)\s*:(.*?)break;", txt, re.S):
            op = res.get(c.group(1), f.stem)
            rp = re.search(r"readPayload\([^,]*,\s*(?:new\s+TypeReference<([^>]+(?:>[^>]*)?)>\(\)\s*\{\s*\}|([\w.]+)\.class)\)", c.group(2))
            payload = (rp.group(1) or rp.group(2)) if rp else None
            ops[op] = {"payload": payload, "handler": f.stem}
        out[topic] = ops
    return out

def parse_dtos():
    dtos, enums = {}, {}
    for f in (COMMON / "data-transfer-objects/src/main/java/eu/arrowhead/dto").rglob("*.java"):
        txt = strip_comments(f.read_text())
        m = re.search(r"public\s+record\s+(\w+)\s*\((.*?)\)\s*(?:implements[^{]*)?\{", txt, re.S)
        if m:
            fields = []
            params = m.group(2)
            depth, cur, items = 0, "", []
            for ch in params:
                if ch == "<": depth += 1
                if ch == ">": depth -= 1
                if ch == "," and depth == 0:
                    items.append(cur); cur = ""
                else:
                    cur += ch
            items.append(cur)
            for it in items:
                it = re.sub(r"@\w+(\([^)]*\))?", "", it).strip()
                if not it:
                    continue
                t, n = it.rsplit(None, 1)
                fields.append({"name": n, "type": " ".join(t.split())})
            dtos[m.group(1)] = fields
            continue
        e = re.search(r"public\s+enum\s+(\w+)\s*\{(.*?)(?:;|\})", txt, re.S)
        if e:
            vals = [re.sub(r"\(.*", "", v).strip() for v in e.group(2).split(",") if v.strip()]
            enums[e.group(1)] = vals
    return dtos, enums

def main():
    const_files = list((COMMON / "common-utils/src/main/java").rglob("Constants.java")) + \
                  list((COMMON / "common-utils/src/main/java").rglob("Defaults.java"))
    for key, (mod, pkg, pref) in SYSTEMS.items():
        const_files += list((CORE / mod / "src/main/java").rglob("*Constants.java"))
        const_files += list((CORE / mod / "src/main/java").rglob("*Defaults.java"))
    model = {"systems": {}}
    for key, (mod, pkg, pref) in SYSTEMS.items():
        res = Resolver(parse_constants([f for f in const_files if "/" + mod + "/" in str(f) or "common-utils" in str(f)]))
        src = CORE / mod / "src/main/java/eu/arrowhead" / pkg
        sysinfo = next(src.glob("*SystemInfo.java"))
        services = parse_sysinfo(sysinfo, res)
        eps = []
        for f in (src / "api/http").glob("*.java"):
            eps += parse_api(f, res)
        mq = parse_mqtt_handlers(src / "api/mqtt", res)
        for s in services:
            if "http" in s:
                for op in s["http"]["ops"]:
                    full = s["http"]["basePath"] + op["path"]
                    norm = lambda p: re.sub(r"\{[^}]*\}", "{}", p)
                    match = [e for e in eps if e["method"] == op["method"] and (e["path"] == full or norm(e["path"]) == norm(full) or e["path"].startswith(full + "/{"))]
                    if match:
                        e = match[0]
                        op.update({k: e[k] for k in ("javaMethod", "summary", "requestBody", "queryParams", "pathVars", "response", "status", "codes", "class")})
                        op["fullPath"] = e["path"]
                        e["matched"] = True
                    else:
                        op["fullPath"] = full
                        op["unmatched"] = True
                        # registered descriptor without an implementation at that path: find the implemented sibling
                        alt = [e for e in eps if e["method"] == op["method"] and not e.get("matched") and full.startswith(e["path"] + "/")]
                        if alt:
                            e = alt[0]
                            op.update({k: e[k] for k in ("javaMethod", "summary", "requestBody", "queryParams", "pathVars", "response", "status", "codes", "class")})
                            op["implementedPath"] = e["path"]
                            e["matched"] = True
            if "mqtt" in s:
                hm = mq.get(s["mqtt"]["baseTopic"], {})
                s["mqtt"]["ops"] = [{"name": o, **hm.get(o, {})} for o in s["mqtt"]["ops"]]
        model["systems"][key] = {"module": mod, "services": services,
                                 "unregisteredEndpoints": [e for e in eps if not e.get("matched")]}
    dtos, enums = parse_dtos()
    model["dtos"] = dtos
    model["enums"] = enums
    json.dump(model, sys.stdout, indent=1)

main()
