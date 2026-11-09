#!/usr/bin/env python3
"""Generate the Arrowhead-template LaTeX documentation (SysD, SysDD, SD, IDD) of the ah5-core-java-spring core systems.

Usage: generate.py <Arrowhead root> <model.json>
"""
import json, os, re, shutil, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from content_fields import FIELD, FIELD_IN, DTO, EXAMPLE, EXAMPLE_IN
from content_services import SERVICE, OP, GM, SYSMONITOR_OPS
from content_systems import SYSTEMS

ROOT = Path(sys.argv[1])
MODEL = json.loads(Path(sys.argv[2]).read_text())
CORE = ROOT / "ah5-core-java-spring"
TEMPLATES = ROOT / "roadmap/Documentation_templates/LaTeX"
VERSION = "5.2.2"
DATE = os.environ.get("DOC_DATE", "2026-10-22")  # document date and revision-history entry
AUTHOR = "Jerker Delsing"
CONTACT = "jerker.delsing@ltu.se"
STATUS = "RELEASE CANDIDATE"
ORDER = ["ServiceRegistry", "Authentication", "ConsumerAuthorization", "DynamicServiceOrchestration", "SimpleStoreServiceOrchestration"]

D = dict(MODEL["dtos"])
D["PriorityRequestDTO"] = []
D["MetadataRequirementDTO"] = []
ENUMS = MODEL["enums"]

# ---------------------------------------------------------------------------------------------- text helpers

SPECIAL = {"\\": r"\textbackslash{}", "&": r"\&", "%": r"\%", "$": r"\$", "#": r"\#", "_": r"\_",
           "{": r"\{", "}": r"\}", "~": r"\textasciitilde{}", "^": r"\textasciicircum{}",
           "<": r"\textless{}", ">": r"\textgreater{}", "|": r"\textbar{}", "\u2026": r"\ldots{}"}

def esc(s):
    return "".join(SPECIAL.get(c, c) for c in str(s))

def code(s):
    # typewriter with break opportunities after separators
    out = esc(s)
    for sep in ["/", r"\textbar{}", ".", r"\_", ",", "-", "="]:
        out = out.replace(sep, sep + r"\allowbreak{}")
    if len(s) > 18:
        out = re.sub(r"(?<=[a-z])(?=[A-Z])", r"\\allowbreak{}", out)
    return r"\texttt{" + out + "}"

def tex(s):
    """Plain text with `code` spans and the \\ldots macro."""
    if s is None:
        return ""
    parts = re.split(r"(`[^`]*`)", s.replace("\\ldots", "\u2026"))
    out = []
    for p in parts:
        if p.startswith("`") and p.endswith("`") and len(p) > 1:
            out.append(code(p[1:-1]))
        else:
            out.append(re.sub(r"[A-Za-z]{18,}", lambda m: brk(m.group(0)), esc(p)))
    return "".join(out)

def paras(s):
    return "\n\n".join(tex(p.strip()) for p in s.split("\n\n") if p.strip())

def brk(s):
    """Escaped identifier with break opportunities at camelCase boundaries."""
    return re.sub(r"(?<=[a-z])(?=[A-Z])", r"\\allowbreak{}", esc(s))

def mref(name):
    n = strip_dto(name)
    return r"\mref[" + brk(n) + "]{" + n + "}" if len(n) > 24 else r"\mref{" + n + "}"

def strip_dto(n):
    return n[:-3] if n.endswith("DTO") else n

def label(s):
    return re.sub(r"[^A-Za-z0-9:-]", "", s)

# ---------------------------------------------------------------------------------------------- type handling

def split_generic(t):
    t = t.strip()
    m = re.match(r"^([\w.]+)\s*<(.*)>$", t)
    if not m:
        return t, []
    args, depth, cur = [], 0, ""
    for ch in m.group(2):
        if ch == "<": depth += 1
        if ch == ">": depth -= 1
        if ch == "," and depth == 0:
            args.append(cur.strip()); cur = ""
        else:
            cur += ch
    args.append(cur.strip())
    return m.group(1), args

def unwrap_response(t):
    if not t:
        return None
    base, args = split_generic(t)
    if base == "ResponseEntity":
        return None if args[0] == "Void" else args[0]
    if t in ("void", "Void"):
        return None
    return t

PRIMS_USED = set()

def string_kind(field):
    f = field or ""
    if re.search(r"(At|Time|From|To|Before|After)$", f) or f in ("from", "to", "alivesAt", "entryDate"):
        return "DateTime"
    if f in ("version",):
        return "Version"
    if f in ("token",):
        return "Token"
    if f in ("id", "instanceId", "serviceInstanceId", "subscriptionId", "orchestrationJobId", "logId", "tokenReference") or f.endswith("Id"):
        return "Identifier"
    if f in ("name", "systemName", "provider", "consumer", "owner", "requester", "requesterSystem", "providerName",
             "targetSystem", "targetSystemName", "ownerSystemName", "deviceName", "serviceDefinition",
             "serviceDefinitionName", "serviceDefinitition", "templateName", "target", "createdBy", "updatedBy",
             "scope", "cloud", "consumerCloud", "cloudIdentitifer"):
        return "Name"
    return "String"

def abstract_type(t, field=None, item=False):
    """SD (abstract) rendering of a Java type."""
    t = t.strip()
    base, args = split_generic(t)
    if base in ("List", "Set"):
        return r"\pref{List}$<$\allowbreak{}" + abstract_type(args[0], field, True) + "$>$" if _p("List") else ""
    if base == "Map":
        if field in ("metadata",):
            _p("Metadata"); return r"\pref{Metadata}"
        _p("Map")
        return r"\pref{Map}$<$\allowbreak{}" + abstract_type(args[0], None, True) + ", " + abstract_type(args[1], None, True) + "$>$"
    if base in D:
        return mref(base)
    if base in ENUMS:
        _p(base); return r"\pref{" + base + "}"
    if base == "String":
        k = string_kind(field if not item else (field[:-1] if field and field.endswith("s") else field))
        _p(k); return r"\pref{" + k + "}"
    if base in ("Integer", "int", "Long", "long"):
        _p("Number"); return r"\pref{Number}"
    if base in ("Boolean", "boolean"):
        _p("Boolean"); return r"\pref{Boolean}"
    if base == "Object":
        _p("Object"); return r"\pref{Object}"
    _p("String"); return r"\pref{String}"

def _p(name):
    PRIMS_USED.add(name)
    return True

JPRIMS_USED = set()

def json_type(t, field=None, item=False):
    """IDD (JSON) rendering of a Java type."""
    t = t.strip()
    base, args = split_generic(t)
    if base in ("List", "Set"):
        JPRIMS_USED.add("Array")
        return r"\pref{Array}$<$\allowbreak{}" + json_type(args[0], field, True) + "$>$"
    if base == "Map":
        JPRIMS_USED.add("Object")
        if args and args[1] == "Object":
            return r"\pref{Object}"
        return r"\pref{Object}$<$\allowbreak{}" + json_type(args[1], None, True) + "$>$"
    if base in D:
        return mref(base)
    if base in ENUMS:
        JPRIMS_USED.add(base); return r"\pref{" + base + "}"
    if base == "String":
        k = string_kind(field if not item else (field[:-1] if field and field.endswith("s") else field))
        k = {"Name": "String", "Token": "String", "Identifier": "String", "Version": "Version"}.get(k, k)
        JPRIMS_USED.add(k); return r"\pref{" + k + "}"
    if base in ("Integer", "int", "Long", "long"):
        JPRIMS_USED.add("Number"); return r"\pref{Number}"
    if base in ("Boolean", "boolean"):
        JPRIMS_USED.add("Boolean"); return r"\pref{Boolean}"
    JPRIMS_USED.add("Object"); return r"\pref{Object}"

def plain_type(t):
    """Short textual type for operation headers."""
    if not t:
        return ""
    base, args = split_generic(t)
    if base in ("List", "Set"):
        return "List<" + plain_type(args[0]) + ">"
    if base in D:
        return strip_dto(base)
    return base

def dto_closure(types):
    out, stack = [], list(types)
    while stack:
        t = stack.pop()
        if not t:
            continue
        for r in re.findall(r"[A-Z]\w+", t):
            if r in D and r not in out:
                out.append(r)
                stack += [f["type"] for f in D[r]]
    return sorted(out, key=strip_dto)

def enums_in(dtos):
    found = set()
    for d in dtos:
        for f in D[d]:
            for r in re.findall(r"[A-Z]\w+", f["type"]):
                if r in ENUMS:
                    found.add(r)
    return sorted(found)

def field_desc(dto, f):
    return FIELD_IN.get((dto, f), FIELD.get(f, ""))

# ---------------------------------------------------------------------------------------------- examples

def example_value(t, field, depth=0):
    base, args = split_generic(t.strip())
    if base in ("List", "Set"):
        v = example_value(args[0], field[:-1] if field and field.endswith("s") else field, depth + 1)
        return [] if v is None else [v]
    if base == "Map":
        if field in ("metadata",):
            return {"unit": "celsius"}
        if field == "properties":
            return {"accessAddresses": ["192.168.0.10"], "accessPort": 8080, "basePath": "/temperature",
                    "operations": {"read": {"method": "GET", "path": "/read"}}}
        if field in ("credentials", "newCredentials"):
            return {"password": "<password>"}
        if field == "orchestrationFlags":
            return {"MATCHMAKING": True}
        if field == "scopedPolicies":
            return {"read": {"policyType": "ALL"}}
        if field == "map":
            return {"server.port": "8443"}
        if field == "requirements":
            return {"maxLatency": 100}
        if field == "authorizationTokens":
            return {}
        return {}
    if base in D:
        if depth > 4:
            return {}
        if base == "MetadataRequirementDTO":
            return {"unit": "celsius"}
        if base == "PriorityRequestDTO":
            return {"3fa85f64-5717-4562-b3fc-2c963f66afa6": 2}
        return {f["name"]: (EXAMPLE_IN[(base, f["name"])] if (base, f["name"]) in EXAMPLE_IN else example_value(f["type"], f["name"], depth + 1))
                for f in D[base] if f["name"] not in ("exception",)}
    if base in ENUMS:
        return ENUMS[base][0]
    if field in EXAMPLE:
        return EXAMPLE[field]
    singular = field[:-1] if field and field.endswith("s") else field
    if base == "String":
        if singular in EXAMPLE:
            return EXAMPLE[singular]
        k = string_kind(field)
        return {"DateTime": "2026-10-22T12:00:00Z", "Version": "1.0.0", "Name": "TemperatureSensor",
                "Identifier": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "Token": "<token>"}.get(k, "string")
    if base in ("Integer", "int", "Long", "long"):
        return 1
    if base in ("Boolean", "boolean"):
        return True
    return None

def prune(v):
    """Drop empty nested values to keep listings short."""
    if isinstance(v, dict):
        return {k: prune(x) for k, x in v.items() if x not in (None, [], {}) or k in ("results", "entries")}
    if isinstance(v, list):
        return [prune(x) for x in v]
    return v

def json_listing(v):
    return json.dumps(prune(v), indent=2)

# ---------------------------------------------------------------------------------------------- preamble

PRE_COMMON = r"""\documentclass[a4paper]{arrowhead}

\usepackage[yyyymmdd]{datetime}
\usepackage{etoolbox}
\usepackage[utf8]{inputenc}
\usepackage[T1]{fontenc}
\usepackage{multirow}
\usepackage{longtable}
\usepackage{tikz}
\usepackage{adjustbox}
\AtBeginEnvironment{longtable}{\small}
\BeforeBeginEnvironment{tikzpicture}{\begin{adjustbox}{max width=\textwidth}}
\AfterEndEnvironment{tikzpicture}{\end{adjustbox}}
\usetikzlibrary{positioning,shapes.multipart,arrows.meta}

\renewcommand{\dateseparator}{-}
\setlength{\emergencystretch}{3em}
\lstset{language=HTTP,breakatwhitespace=false,columns=fullflexible}
"""

PRE_SD = r"""
%% Special references
\newcommand{\mref}[2][]{{\textcolor{ArrowheadPurple}{\hyperref[sec:model:#2]{\ifstrempty{#1}{#2}{#1}}}}}
\newcommand{\oref}[1]{{\textcolor{ArrowheadBlue}{\hyperref[sec:operations:#1]{#1}}}}
\newcommand{\pdef}[1]{{\textcolor{ArrowheadGrey}{#1\label{sec:model:primitives:#1}\label{sec:model:primitives:#1s}\label{sec:model:primitives:#1es}}}}
\newcommand{\pref}[1]{{\textcolor{ArrowheadGrey}{\hyperref[sec:model:primitives:#1]{#1}}}}

% as template_sd.tex; the input and output arguments are passed already formatted (\mref or \pref)
\newrobustcmd\osubsection[3]{
  \addtocounter{subsection}{1}
  \addcontentsline{toc}{subsection}{\protect\numberline{\thesubsection}operation \textcolor{ArrowheadBlue}{#1}}
  \subsection*{
    \thesubsection\quad
    operation
    \textcolor{ArrowheadBlue}{#1}
    (\notblank{#2}{#2}{})
    \notblank{#3}{: #3}{}
  }
  \label{sec:operations:#1}
}
\newrobustcmd\msubsection[2]{
  \addtocounter{subsection}{1}
  \addcontentsline{toc}{subsection}{\protect\numberline{\thesubsection}#1 \textcolor{ArrowheadPurple}{#2}}
  \subsection*{\thesubsection\quad#1 \textcolor{ArrowheadPurple}{#2}}
  \label{sec:model:#2} \label{sec:model:#2es}
}
%%
"""

PRE_IDD = r"""
\newcommand{\fparam}[1]{\textit{\textcolor{ArrowheadBlue}{#1}}}

%% Special references
\newcommand{\mref}[2][]{{\textcolor{ArrowheadPurple}{\hyperref[sec:model:#2]{\ifstrempty{#1}{#2}{#1}}}}}
\newcommand{\oref}[1]{{\textcolor{ArrowheadBlue}{\hyperref[sec:operations:#1]{#1}}}}
\newcommand{\pdef}[1]{{\textcolor{ArrowheadGrey}{#1 \label{sec:model:primitives:#1} \label{sec:model:primitives:#1s}}}}
\newcommand{\pref}[1]{{\textcolor{ArrowheadGrey}{\hyperref[sec:model:primitives:#1]{#1}}}}

% as template_idd.tex; the input and output arguments are passed already formatted (\mref or \pref)
\newrobustcmd\osubsection[5]{
  \addtocounter{subsection}{1}
  \addcontentsline{toc}{subsection}{\protect\numberline{\thesubsection}operation \textcolor{ArrowheadBlue}{#1}}
  \subsection*{
    \thesubsection\quad
    #2 \textcolor{ArrowheadPurple}{#3} \\
    \small
    \hspace*{0.075\textwidth}\begin{minipage}{0.1\textwidth}
      \vspace*{1mm}
      Operation: \\
      \notblank{#4}{Input: \\}{}
      \notblank{#5}{Output: \\}{}
    \end{minipage}%
    \begin{minipage}{0.825\textwidth}
      \vspace*{1mm}
      \textcolor{ArrowheadBlue}{#1} \\
      \notblank{#4}{#4 \\}{}
      \notblank{#5}{#5 \\}{}
    \end{minipage}
  }
  \label{sec:operations:#1}
}
\newrobustcmd\msubsection[2]{
  \addtocounter{subsection}{1}
  \addcontentsline{toc}{subsection}{\protect\numberline{\thesubsection}#1 \textcolor{ArrowheadPurple}{#2}}
  \subsection*{\thesubsection\quad#1 \textcolor{ArrowheadPurple}{#2}}
  \label{sec:model:#2}
}
%%
"""

PRE_SYS = r"""
%% Special references
\newcommand{\mref}[2][]{{\textcolor{ArrowheadPurple}{\hyperref[sec:model:#2]{\ifstrempty{#1}{#2}{#1}}}}}
\newcommand{\pref}[1]{{\textcolor{ArrowheadGrey}{\hyperref[sec:model:primitives:#1]{#1}}}}
%%
"""

def properties(title, sid, typ, short):
    sid_line = f"\\ArrowheadServiceID{{{esc(sid)}}}\n" if sid else ""
    return f"""
\\begin{{document}}
\\sloppy

%% Arrowhead Document Properties
\\ArrowheadTitle{{{title}}}
{sid_line}\\ArrowheadType{{{typ}}}
\\ArrowheadTypeShort{{{short}}}
\\ArrowheadVersion{{{VERSION}}}
\\ArrowheadDate{{{DATE}}}
\\ArrowheadAuthor{{{AUTHOR}}}
\\ArrowheadStatus{{{STATUS}}}
\\ArrowheadContact{{{CONTACT}}}
\\ArrowheadFooter{{\\href{{www.arrowhead.eu}}{{www.arrowhead.eu}}}}
\\ArrowheadSetup
%%
"""

def front(abstract, sid=None, figure=""):
    sid_line = f"\n  \\Large{{Service ID: \\textit{{\"\\arrowid\"}}}}\n" if sid else ""
    return f"""
%% Front Page
\\begin{{center}}
  \\vspace*{{1cm}}
  \\huge{{\\arrowtitle}}

  \\vspace*{{0.2cm}}
  \\LARGE{{\\arrowtype}}
  \\vspace*{{1cm}}
{sid_line}
  \\vspace*{{\\fill}}

  % Front Page Image
{figure}
  \\vspace*{{1cm}}
  \\vspace*{{\\fill}}

  % Front Page Abstract
  \\begin{{abstract}}
    {abstract}
  \\end{{abstract}}

  \\vspace*{{1cm}}
\\end{{center}}

\\newpage
%%

%% Table of Contents
\\tableofcontents
\\newpage
%%
"""

def ending(subject):
    return f"""
\\bibliographystyle{{IEEEtran}}
\\bibliography{{bibliography}}

\\newpage

\\section{{Revision History}}
\\subsection{{Amendments}}

\\noindent\\begin{{tabularx}}{{\\textwidth}}{{| p{{1cm}} | p{{3cm}} | p{{2cm}} | X | p{{4cm}} |}} \\hline
\\rowcolor{{gray!33}} No. & Date & Version & Subject of Amendments & Author \\\\ \\hline

1 & {DATE} & \\arrowversion & {subject} & {AUTHOR} \\\\ \\hline
\\end{{tabularx}}

\\subsection{{Quality Assurance}}

\\noindent\\begin{{tabularx}}{{\\textwidth}}{{| p{{1cm}} | p{{3cm}} | p{{2cm}} | X |}} \\hline
\\rowcolor{{gray!33}} No. & Date & Version & Approved by \\\\ \\hline

1 & & \\arrowversion  &  \\\\ \\hline

\\end{{tabularx}}

\\end{{document}}
"""

SUBJECT = "Document created from the Arrowhead LaTeX templates for the 5.2.2 Java Spring implementation, including the local extensions of this workspace."

# ---------------------------------------------------------------------------------------------- names, references

def docname(system, kind, svc=None, binding=None):
    if svc:
        return f"{system}-v{VERSION}-{svc}-" + (f"{binding}-IDD" if binding else "SD")
    return f"{system}-v{VERSION}-{kind}"

def bibkey(system, kind, svc=None, binding=None):
    s = SYSTEMS[system]["short"].lower()
    if svc:
        return f"{s}-{svc}-" + (f"{binding.lower()}-idd" if binding else "sd")
    return f"{s}-{kind.lower()}"

def services_of(system):
    svcs = [s for s in MODEL["systems"][system]["services"]]
    order = {"generalManagement": 99}
    svcs.sort(key=lambda s: order.get(s["serviceDefinition"], 0))
    return svcs

def svc_text(system, sd, key):
    d = dict(SERVICE.get(sd, {}))
    d.update(SERVICE.get((system, sd), {}))
    return d.get(key, "").format(system=system)

def op_text(system, sd, op):
    return OP.get((system, sd, op)) or OP.get((sd, op)) or ""

def status_text(op):
    resp = op.get("response") or ""
    base, args = split_generic(resp)
    name = op["name"]
    if base == "ResponseEntity" and args and args[0] == "Void":
        return "\\texttt{200 OK} if the entry was removed, \\texttt{204 No Content} if there was nothing to remove"
    if base == "ResponseEntity":
        return "\\texttt{201 Created} for a new entry, \\texttt{200 OK} if an identical entry already existed"
    if op.get("status") == "CREATED":
        return "\\texttt{201 Created}"
    return "\\texttt{200 OK}"

def http_scheme_note():
    return ("All operations require the requester's identity, presented according to the provider's authentication policy: "
            "the client certificate (mutual TLS) and, with the \\texttt{outsourced} policy, "
            "\\texttt{Authorization: Bearer IDENTITY-TOKEN//<token>}; systems holding the Authentication secret key send "
            "\\texttt{Bearer AUTHENTICATOR-KEY//<system>//<hash>}, and with the \\texttt{declared} policy "
            "\\texttt{Bearer SYSTEM//<system>} is sent.")

# ---------------------------------------------------------------------------------------------- tables

def model_table_sd(dto):
    rows = []
    for f in D[dto]:
        rows.append(f"{brk(f['name'])} & {abstract_type(f['type'], f['name'])} & {tex(field_desc(dto, f['name']))} \\\\ \\hline")
    return ("\\noindent\\begin{tabularx}{\\textwidth}{| >{\\raggedright\\arraybackslash}p{3.9cm} | >{\\raggedright\\arraybackslash\\small}p{5.3cm} | X |} \\hline\n"
            "\\rowcolor{gray!33} Field & Type & Description \\\\ \\hline\n" + "\n".join(rows) + "\n\\end{tabularx}\n")

def model_table_idd(dto):
    rows = []
    for f in D[dto]:
        rows.append(f"\"{brk(f['name'])}\" & {json_type(f['type'], f['name'])} & {tex(field_desc(dto, f['name']))} \\\\ \\hline")
    return ("\\noindent\\begin{tabularx}{\\textwidth}{| >{\\raggedright\\arraybackslash}p{3.9cm} | >{\\raggedright\\arraybackslash\\small}p{5.3cm} | X |} \\hline\n"
            "\\rowcolor{gray!33} Object Field & Value Type & Description \\\\ \\hline\n" + "\n".join(rows) + "\n\\end{tabularx}\n")

def struct_sections(dtos, idd):
    out = []
    for d in dtos:
        out.append(f"\\msubsection{{struct}}{{{strip_dto(d)}}}\n")
        out.append(tex(DTO.get(d, "")) + "\n")
        if D[d]:
            out.append("\n" + (model_table_idd(d) if idd else model_table_sd(d)))
        out.append("\n")
    return "\n".join(out)

SD_PRIMS = {
    "Boolean": "One out of \\texttt{true} or \\texttt{false}.",
    "DateTime": "Pinpoints a specific moment in time (UTC).",
    "Identifier": "A string that identifies a record, e.g. a UUID or a composite identifier.",
    "List": "An \\textit{array} of a known number of items, each having type A.",
    "Map": "A set of key-value pairs.",
    "Metadata": "A set of key-value pairs with arbitrary, possibly nested values.",
    "Name": "A string identifier that is intended to be both human and machine-readable.",
    "Number": "An integer number.",
    "Object": "Any structured value.",
    "String": "A chain of characters.",
    "Token": "An opaque string used as credential.",
    "Version": "Specifies a version, semantic versioning \\texttt{major.minor.patch}.",
}

JSON_PRIMS = {
    "Array": "A JSON Array \\cite{rfc8259}; \\pref{Array}$<$A$>$ has elements of type A.",
    "Boolean": "JSON Boolean, \\texttt{true} or \\texttt{false}.",
    "DateTime": "JSON String with an ISO 8601 UTC timestamp, e.g. \\texttt{2026-10-22T12:00:00Z}.",
    "Number": "JSON Number, an integer.",
    "Object": "JSON Object; \\pref{Object}$<$A$>$ has string keys and values of type A.",
    "String": "JSON String.",
    "Version": "JSON String, semantic version \\texttt{major.minor.patch}.",
}

def prim_table(used, defs, idd):
    rows = []
    for p in sorted(used):
        if p in ENUMS:
            d = "One of " + ", ".join(code(v) for v in ENUMS[p]) + (" (JSON String)." if idd else ".")
        else:
            d = defs.get(p, "")
        rows.append(f"\\pdef{{{p}}} & {d} \\\\ \\hline")
    return ("\\noindent\\begin{tabularx}{\\textwidth}{| p{4.6cm} | X |} \\hline\n"
            "\\rowcolor{gray!33} Type & Description \\\\ \\hline\n" + "\n".join(rows) + "\n\\end{tabularx}\n")

# ---------------------------------------------------------------------------------------------- figures

def block_figure(title, stereotype, items, caption, lab):
    lines = "\\\\ ".join(code(i) for i in items) if items else "--"
    return f"""
\\begin{{figure}}[ht!]
  \\centering
  \\begin{{tikzpicture}}[font=\\small]
    \\node[draw=ArrowheadBlue, very thick, rectangle split, rectangle split parts=2, align=center,
          rectangle split part fill={{ArrowheadBlue!12,white}}, text width=9cm, inner sep=6pt] (b)
      {{\\guillemotleft{{}}{esc(stereotype)}\\guillemotright{{}}\\\\\\textbf{{{esc(title)}}}
        \\nodepart{{second}} {lines}}};
  \\end{{tikzpicture}}
  \\caption{{{caption}}}
  \\label{{{lab}}}
\\end{{figure}}
"""

def system_figure(system, produced, consumed):
    left = "\\\\ ".join(code(p) for p in produced)
    right = "\\\\ ".join(esc(c) for c in consumed)
    return f"""
\\begin{{figure}}[ht!]
  \\centering
  \\begin{{tikzpicture}}[font=\\small, >=Stealth]
    \\node[draw=ArrowheadBlue, very thick, minimum width=4.2cm, minimum height=2.2cm, align=center, fill=ArrowheadBlue!10] (s)
      {{\\guillemotleft{{}}System\\guillemotright{{}}\\\\\\textbf{{{esc(system)}}}}};
    \\node[draw=ArrowheadTeal, rectangle, align=left, left=1.3cm of s, text width=4.6cm, inner sep=5pt] (p)
      {{\\textbf{{Produced services}}\\\\ {left}}};
    \\node[draw=ArrowheadOrange, rectangle, align=left, right=1.3cm of s, text width=4.6cm, inner sep=5pt] (c)
      {{\\textbf{{Consumed}}\\\\ {right}}};
    \\draw[->, thick, ArrowheadTeal] (s) -- (p);
    \\draw[->, thick, ArrowheadOrange] (s) -- (c);
  \\end{{tikzpicture}}
  \\caption{{SysML-style block view of {esc(system)} with its produced and consumed services.}}
  \\label{{fig:system}}
\\end{{figure}}
"""

# ---------------------------------------------------------------------------------------------- SD

def op_io(op):
    """Input and output DTO names of an HTTP operation (None if none)."""
    inp = op.get("requestBody")
    if not inp:
        if op.get("pathVars"):
            inp = "String"
        elif op.get("queryParams"):
            req = [q for q in op["queryParams"] if q["required"]]
            inp = req[0]["type"] if req else None
    return inp, unwrap_response(op.get("response"))

def op_param_field(op):
    if op.get("pathVars"):
        return op["pathVars"][0]
    req = [q for q in op.get("queryParams", []) if q["required"]]
    return req[0]["name"] if req else None

def sd_doc(system, svc):
    PRIMS_USED.clear()
    sd = svc["serviceDefinition"]
    binding_refs = f"The concrete interface is specified in the HTTP/TLS/JSON IDD \\cite{{{bibkey(system, '', sd, 'HTTP')}}}."
    if "mqtt" in svc:
        binding_refs += f" The MQTT binding is specified in the MQTT/TLS/JSON IDD \\cite{{{bibkey(system, '', sd, 'MQTT')}}}."
    ops = svc.get("http", {}).get("ops", [])
    types = []
    for o in ops:
        i, r = op_io(o)
        types += [i, r]
    dtos = dto_closure(types)
    purpose = svc_text(system, sd, "purpose")
    op_sections = []
    for o in ops:
        i, r = op_io(o)
        field = op_param_field(o) if not o.get("requestBody") else None
        it = (abstract_type(i, field) if i else "")
        rt = (abstract_type(r) if r else "")
        op_sections.append(f"\\osubsection{{{esc(o['name'])}}}{{{it}}}{{{rt}}}\n\n{tex(op_text(system, sd, o['name']))}\n")
        if o.get("queryParams") and o.get("requestBody"):
            opt = ", ".join(code(q["name"]) for q in o["queryParams"])
            op_sections.append(f"Optional parameter: {opt}.\n")
    op_names = [o["name"] for o in ops]
    fig = block_figure(sd, "Service", op_names, f"SysML-style block of the {code(sd)} service and its operations.", "fig:SD")
    struct = struct_sections(dtos, False)
    prims = prim_table(PRIMS_USED | enum_set(dtos), SD_PRIMS, False)
    prior = svc_text(system, sd, "prior")
    body = f"""{PRE_COMMON}{PRE_SD}{properties(esc(sd), sd, "Service Description", "SD")}{front(f"This document describes the {code(sd)} service, version {esc(svc['version'])}, produced by the {esc(system)} core system of Arrowhead {VERSION}.", sd)}
\\section{{Overview}}
\\label{{sec:overview}}

This document describes the {code(sd)} service, which provides {tex(purpose)}.
It is produced by the {esc(system)} system \\cite{{{bibkey(system, 'SysD')}}} in service version {esc(svc['version'])}.

The rest of this document is organized as follows.
In Section \\ref{{sec:operations}}, we describe the abstract message operations provided by the service.
In Section \\ref{{sec:model}}, we end the document by presenting the data types used by the mentioned operations.
{binding_refs}

\\subsection{{Significant Prior Art}}

{tex(prior)}

\\subsection{{How This Service Is Meant to Be Used}}

{paras(svc_text(system, sd, "use"))}

\\subsection{{Important Delimitations and Dependencies}}
\\label{{sec:delimitations}}

{paras(svc_text(system, sd, "limits"))}

\\newpage

\\section{{Service Interface}}
\\label{{sec:operations}}

This section describes the interfaces to the {code(sd)} service.
In particular, each subsection names an abstract operation, an input type and an output type, in that order.
The input type is named inside parentheses, while the output type is preceded by a colon.
Input and output types are only denoted when accepted or returned, respectively, by the interface in question.

All abstract data types named in this section are defined in Section \\ref{{sec:model}}.
{fig}
The following interface operations are available.

{chr(10).join(op_sections)}
\\newpage

\\section{{Information Model}}
\\label{{sec:model}}

Here, all data objects that can be part of the {code(sd)} service are listed in alphabetic order.
Note that each subsection, which describes one type of object, begins with the \\textit{{struct}} keyword, which is used to denote a collection of named fields, each with its own data type.
As a complement to the explicitly defined types in this section, there is also a list of implicit primitive types in Section \\ref{{sec:model:primitives}}, which are used to represent things like hashes and identifiers.

{struct}
\\subsection{{Primitives}}
\\label{{sec:model:primitives}}

Types and structures mentioned throughout this document that are assumed to be available to implementations of this service.
The concrete interpretations of each of these types and structures must be provided by any IDD document claiming to implement this service.

{prims}
\\newpage
{ending(SUBJECT)}"""
    return body

def enum_set(dtos):
    return set(enums_in(dtos))

# ---------------------------------------------------------------------------------------------- HTTP IDD

def http_path(p):
    out = esc(p)
    out = re.sub(r"\\\{(\w+)\\\}", lambda m: r"\fparam{\{" + m.group(1) + r"\}}", out)
    return out.replace("/", "/\\allowbreak{}")

def http_idd(system, svc):
    JPRIMS_USED.clear()
    JPRIMS_USED.update({"Object", "String", "Number"})
    sd = svc["serviceDefinition"]
    h = svc["http"]
    ops = h["ops"]
    types = []
    for o in ops:
        i, r = op_io(o)
        types += [i, r]
    dtos = dto_closure(types + ["ErrorMessageDTO"]) if "ErrorMessageDTO" in D else dto_closure(types)
    sysd = SYSTEMS[system]
    rows = []
    sections = []
    for o in ops:
        path = o.get("implementedPath") or o["fullPath"]
        reg = h["basePath"] + o["path"]
        rows.append(f"{code(o['name'])} & {esc(o['method'])} & {code(o['path'])} \\\\ \\hline")
        i, r = op_io(o)
        field = op_param_field(o) if not o.get("requestBody") else None
        it = json_type(i, field) if i else ""
        rt = json_type(r) if r else ""
        qs = o.get("queryParams", [])
        disp = path + ("?" + "&".join(f"{q['name']}={{{q['name']}}}" for q in qs) if qs else "")
        sec = [f"\\osubsection{{{esc(o['name'])}}}{{{esc(o['method'])}}}{{{http_path(disp)}}}{{{it}}}{{{rt}}}\n",
               tex(op_text(system, sd, o["name"])) + "\n"]
        if o.get("pathVars"):
            sec.append("Path parameter: " + ", ".join(f"\\fparam{{{esc(p)}}}" for p in o["pathVars"]) + ".\n")
        if qs:
            items = []
            for q in qs:
                extra = "required" if q["required"] else ("optional" + (f", default {code(q['default'])}" if q.get("default") else ""))
                items.append(f"\\fparam{{{esc(q['name'])}}} ({esc(plain_type(q['type']))}, {extra})")
            sec.append("Query parameters: " + "; ".join(items) + ". List values are repeated or comma-separated.\n")
        if o.get("implementedPath"):
            sec.append(f"\\textbf{{Note.}} The registered interface descriptor of this operation has the path {code(reg)}, "
                       f"but the implementation serves {code(o['implementedPath'])} with the query parameter "
                       f"\\fparam{{{esc(qs[0]['name'] if qs else 'ids')}}}. Consumers must use the implemented form; this is an upstream 5.2.2 inconsistency.\n")
        sec.append(f"A successful call returns {status_text(o)}" + (" with the output in the body." if r else ".") + "\n")
        lab = label(f"lst:{o['name']}")
        req_lines = [f"{o['method']} {disp.replace('{', '<').replace('}', '>')} HTTP/1.1",
                     f"Host: {sysd['host']}:{sysd['port']}", "Authorization: Bearer IDENTITY-TOKEN//<token>"]
        if o.get("requestBody"):
            req_lines.append("Content-Type: application/json")
            req_lines.append("")
            req_lines.append(json_listing(example_value(o["requestBody"], None)))
        sec.append(f"\\begin{{lstlisting}}[label={{{lab}}},caption={{An \\oref{{{esc(o['name'])}}} invocation.}}]\n" + "\n".join(req_lines) + "\n\\end{lstlisting}\n")
        if r:
            sec.append(f"\\begin{{lstlisting}}[label={{{lab}-resp}},caption={{An \\oref{{{esc(o['name'])}}} response.}}]\n" +
                       json_listing(example_value(r, None)) + "\n\\end{lstlisting}\n")
        sections.append("\n".join(sec))
    struct = struct_sections(dtos, True)
    prims = prim_table(JPRIMS_USED | enum_set(dtos), JSON_PRIMS, True)
    abstract = f"This document describes the HTTP/1.1 protocol with TLS payload security and JSON payload encoding variant of the {code(sd)} service of the {esc(system)} system."
    return f"""{PRE_COMMON}{PRE_IDD}{properties(esc(sd) + " HTTP/TLS/JSON", sd, "Interface Design Description", "IDD")}{front(abstract, sd)}
\\section{{Overview}}
\\label{{sec:overview}}

This document describes the {code(sd)} service interface of the {esc(system)} system, which provides {tex(svc_text(system, sd, "purpose"))}.

It's implemented using protocol, encoding as stated in below table:
\\begin{{table}}[ht!]
  \\centering
  \\begin{{tabular}}{{|l|l|l|}}
    \\hline
    \\rowcolor{{gray!33}} Profile type & Type & Version \\\\ \\hline
    Transfer protocol & HTTP & 1.1 \\cite{{rfc9112}} \\\\ \\hline
    Data encryption & TLS & 1.3 \\cite{{rfc8446}}, 1.2 \\cite{{rfc5246}} \\\\ \\hline
    Encoding & JSON & RFC 8259 \\cite{{rfc8259}} \\\\ \\hline
    Compression & N/A & - \\\\ \\hline
    Semantics & N/A & - \\\\ \\hline
    Ontology & N/A & - \\\\ \\hline
  \\end{{tabular}}
  \\caption{{Communication and semantics details used for the {code(sd)} service interface}}
  \\label{{tab:comunication_semantics_profile}}
\\end{{table}}

This document provides the Interface Design Description IDD to the \\textit{{{esc(sd)} -- Service Description}} document \\cite{{{bibkey(system, '', sd)}}}.
For further details about how this service is meant to be used, please consult that document.

The rest of this document describes how to realize the {code(sd)} service
interface in detail. Both in terms of its operations (Section \\ref{{sec:operations}}) and its information model (Section \\ref{{sec:model}}).

\\subsection{{Interface registration}}

The service is registered in the ServiceRegistry with the interface template {code('generic_https')} ({code('generic_http')} when TLS is disabled), access port {sysd['port']} and base path {code(h['basePath'])}. The \\texttt{{operations}} property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \\texttt{{\\{{id\\}}}} that the consumer fills in):

\\noindent\\begin{{longtable}}{{| p{{5.2cm}} | p{{1.6cm}} | p{{8.4cm}} |}} \\hline
\\rowcolor{{gray!33}} Operation & Method & Path \\\\ \\hline \\endhead
{chr(10).join(rows)}
\\end{{longtable}}

\\newpage

\\section{{Service Operations}}
\\label{{sec:operations}}

The interfaces of the {code(sd)} service, its operations, data models and implementation are provided below.
In particular, each subsection first names the HTTP method and path used to call the operation, after which it names the abstract operation from the {code(sd)} SD document, as well as input and output types.

{http_scheme_note()} Request and response bodies are \\texttt{{application/json}}.
Errors are answered with an \\mref{{ErrorMessage}} body and the status codes \\texttt{{400 Bad Request}} (invalid parameter), \\texttt{{401 Unauthorized}} (missing or invalid identity), \\texttt{{403 Forbidden}} (not permitted, e.g. by the management or blacklist filter), \\texttt{{404 Not Found}}, \\texttt{{408 Request Timeout}}, \\texttt{{423 Locked}}, \\texttt{{500 Internal Server Error}} and \\texttt{{503 Service Unavailable}} (a system it depends on failed).

{chr(10).join(sections)}
\\newpage

\\section{{Data Models}}
\\label{{sec:model}}

Here, all data objects that can be part of the service calls associated with this service are listed in alphabetic order.
Note that each subsection, which describes one type of object, begins with the \\textit{{struct}} keyword, which is meant to denote a JSON \\pref{{Object}} that must contain certain fields, or names, with values conforming to explicitly named types.
Fields that are not needed may be omitted or \\texttt{{null}} in requests; empty fields are omitted in responses.
As a complement to the primary types defined in this section, there is also a list of secondary types in Section \\ref{{sec:model:primitives}}, which are used to represent things like hashes, identifiers and texts.

{struct}
\\subsection{{Primitives}}
\\label{{sec:model:primitives}}

As all messages are encoded using the JSON format \\cite{{rfc8259}}, the following primitive constructs, part of that standard, become available.
Note that the official standard is defined in terms of parsing rules, while this list only concerns syntactic information.
Furthermore, the \\pref{{Object}} and \\pref{{Array}} types are given optional generic type parameters, which are used in this document to signify when pair values or elements are expected to conform to certain types.

{prims}
\\newpage
{ending(SUBJECT)}"""

# ---------------------------------------------------------------------------------------------- MQTT IDD

def mqtt_idd(system, svc):
    JPRIMS_USED.clear()
    JPRIMS_USED.update({"Object", "String", "Number"})
    sd = svc["serviceDefinition"]
    m = svc["mqtt"]
    http_ops = {o["name"]: o for o in svc.get("http", {}).get("ops", [])}
    rows, sections, types = [], [], []
    for o in m["ops"]:
        hop = http_ops.get(o["name"], {})
        r = unwrap_response(hop.get("response"))
        p = o.get("payload")
        types += [p, r]
        topic = m["baseTopic"] + o["name"]
        rows.append(f"{code(o['name'])} & {code(topic)} \\\\ \\hline")
        pt = json_type(p, op_param_field(hop) if hop and not hop.get("requestBody") else None) if p else ""
        rt = json_type(r) if r else ""
        sec = [f"\\osubsection{{{esc(o['name'])}}}{{PUBLISH}}{{{code(topic)}}}{{{pt}}}{{{rt}}}\n",
               tex(op_text(system, sd, o["name"])) + "\n"]
        qs = hop.get("queryParams", []) if hop else []
        if qs and hop.get("requestBody"):
            sec.append("Optional entries of \\texttt{params}: " + ", ".join(code(q["name"]) for q in qs) + ".\n")
        if p:
            ex = example_value(p, op_param_field(hop) if hop and not hop.get("requestBody") else None)
            sec.append(f"\\begin{{lstlisting}}[caption={{Payload of an \\oref{{{esc(o['name'])}}} request.}}]\n{json_listing(ex)}\n\\end{{lstlisting}}\n")
        sections.append("\n".join(sec))
    dtos = dto_closure(types)
    struct = struct_sections(dtos, True)
    prims = prim_table(JPRIMS_USED | enum_set(dtos) | enum_set(dto_closure(["ErrorMessageDTO"])), JSON_PRIMS, True)
    abstract = f"This document describes the MQTT protocol with TLS payload security and JSON payload encoding variant of the {code(sd)} service of the {esc(system)} system."
    req_example = json.dumps({"traceId": "a1b2c3", "authentication": "IDENTITY-TOKEN//<token>",
                              "responseTopic": "arrowhead/response/TemperatureDisplay/a1b2c3",
                              "qosRequirement": 1, "params": {}, "payload": {}}, indent=2)
    resp_example = json.dumps({"status": 200, "traceId": "a1b2c3", "receiver": "TemperatureDisplay", "payload": {}}, indent=2)
    return f"""{PRE_COMMON}{PRE_IDD}{properties(esc(sd) + " MQTT/TLS/JSON", sd, "Interface Design Description", "IDD")}{front(abstract, sd)}
\\section{{Overview}}
\\label{{sec:overview}}

This document describes the {code(sd)} service interface of the {esc(system)} system over MQTT, which provides {tex(svc_text(system, sd, "purpose"))}.

It's implemented using protocol, encoding as stated in below table:
\\begin{{table}}[ht!]
  \\centering
  \\begin{{tabular}}{{|l|l|l|}}
    \\hline
    \\rowcolor{{gray!33}} Profile type & Type & Version \\\\ \\hline
    Transfer protocol & MQTT & 3.1.1 \\cite{{mqtt311}} \\\\ \\hline
    Data encryption & TLS & 1.3 \\cite{{rfc8446}}, 1.2 \\cite{{rfc5246}} \\\\ \\hline
    Encoding & JSON & RFC 8259 \\cite{{rfc8259}} \\\\ \\hline
    Compression & N/A & - \\\\ \\hline
    Semantics & N/A & - \\\\ \\hline
    Ontology & N/A & - \\\\ \\hline
  \\end{{tabular}}
  \\caption{{Communication and semantics details used for the {code(sd)} service interface}}
  \\label{{tab:comunication_semantics_profile}}
\\end{{table}}

This document provides the Interface Design Description IDD to the \\textit{{{esc(sd)} -- Service Description}} document \\cite{{{bibkey(system, '', sd)}}}.
The operations, their semantics and the data models are the same as in the HTTP/TLS/JSON IDD \\cite{{{bibkey(system, '', sd, 'HTTP')}}}; this document specifies the MQTT binding.

The MQTT interface is offered only when the system runs with \\texttt{{mqtt.api.enabled=true}}; it is then registered with the interface template {code('generic_mqtts')} ({code('generic_mqtt')} without TLS), the broker address and port ({code('mqtt.broker.address')}, {code('mqtt.broker.port')}) and the base topic {code(m['baseTopic'])}. In the Arrowhead-test-cloud-java deployment the MQTT interfaces of the core systems are disabled.

\\newpage

\\section{{Service Operations}}
\\label{{sec:operations}}

The provider subscribes to one topic per operation, the base topic followed by the operation name:

\\noindent\\begin{{longtable}}{{| p{{5cm}} | p{{10.4cm}} |}} \\hline
\\rowcolor{{gray!33}} Operation & Request topic \\\\ \\hline \\endhead
{chr(10).join(rows)}
\\end{{longtable}}

A consumer publishes a request envelope (\\mref{{MqttRequestTemplate}}) to the topic and receives the answer (\\mref{{MqttResponseTemplate}}) on its own \\texttt{{responseTopic}}. The \\texttt{{authentication}} field carries the requester's identity according to the provider's authentication policy: \\texttt{{IDENTITY-TOKEN//<token>}} (outsourced or internal policy), \\texttt{{SYSTEM//<name>}} (declared policy) or the requester's X.509 certificate (certificate policy). The \\texttt{{status}} of the response uses the HTTP status code numbers (200, 201, 204, 400, 401, 403, 404, 408, 423, 500, 503); on error the payload is an \\mref{{ErrorMessage}}.

\\begin{{lstlisting}}[caption={{Request envelope.}}]
{req_example}
\\end{{lstlisting}}

\\begin{{lstlisting}}[caption={{Response envelope.}}]
{resp_example}
\\end{{lstlisting}}

{chr(10).join(sections)}
\\newpage

\\section{{Data Models}}
\\label{{sec:model}}

Here, all data objects that can be part of the service calls associated with this service are listed in alphabetic order.
Each \\textit{{struct}} denotes a JSON \\pref{{Object}} with the named fields; fields that are not needed may be omitted.

\\msubsection{{struct}}{{MqttRequestTemplate}}

The request envelope.

\\noindent\\begin{{tabularx}}{{\\textwidth}}{{| p{{4.25cm}} | p{{3.5cm}} | X |}} \\hline
\\rowcolor{{gray!33}} Object Field & Value Type & Description \\\\ \\hline
"traceId" & \\pref{{String}} & Identifier chosen by the requester, returned in the response. \\\\ \\hline
"authentication" & \\pref{{String}} & Identity of the requester (see Section \\ref{{sec:operations}}). \\\\ \\hline
"responseTopic" & \\pref{{String}} & Topic on which the response is published. \\\\ \\hline
"qosRequirement" & \\pref{{Number}} & MQTT QoS of the response (0, 1 or 2). \\\\ \\hline
"params" & \\pref{{Object}}$<$\\pref{{String}}$>$ & Optional parameters (the query parameters of the HTTP binding). \\\\ \\hline
"payload" & \\pref{{Object}} & The operation input. \\\\ \\hline
\\end{{tabularx}}

\\msubsection{{struct}}{{MqttResponseTemplate}}

The response envelope.

\\noindent\\begin{{tabularx}}{{\\textwidth}}{{| p{{4.25cm}} | p{{3.5cm}} | X |}} \\hline
\\rowcolor{{gray!33}} Object Field & Value Type & Description \\\\ \\hline
"status" & \\pref{{Number}} & Status code. \\\\ \\hline
"traceId" & \\pref{{String}} & The trace identifier of the request. \\\\ \\hline
"receiver" & \\pref{{String}} & Name of the requester. \\\\ \\hline
"payload" & \\pref{{Object}} & The operation output, or an \\mref{{ErrorMessage}}. \\\\ \\hline
\\end{{tabularx}}

{struct_sections(dto_closure(['ErrorMessageDTO']), True) if 'ErrorMessageDTO' in D else ''}
{struct}
\\subsection{{Primitives}}
\\label{{sec:model:primitives}}

As all messages are encoded using the JSON format \\cite{{rfc8259}}, the following primitive constructs, part of that standard, become available.

{prims}
\\newpage
{ending(SUBJECT)}"""

# ---------------------------------------------------------------------------------------------- sysMonitor SD and AMQP IDD

def sysmonitor_sd(system):
    PRIMS_USED.clear()
    rows = "\n".join(f"\\osubsection{{{o}}}{{}}{{\\mref{{Observation}}}}\n\n{tex(t)}\n" for o, t in SYSMONITOR_OPS)
    fig = block_figure("sysMonitor", "Service", [o for o, _ in SYSMONITOR_OPS], "SysML-style block of the \\texttt{sysMonitor} service and its observation types.", "fig:SD")
    return f"""{PRE_COMMON}{PRE_SD}{properties("sysMonitor", "sysMonitor", "Service Description", "SD")}{front(f"This document describes the native {code('sysMonitor')} service, version 1.0.0, produced by the {esc(system)} core system of Arrowhead {VERSION}.", "sysMonitor")}
\\section{{Overview}}
\\label{{sec:overview}}

This document describes the {code('sysMonitor')} service, which provides {tex(svc_text(system, 'sysMonitor', 'purpose'))}.
The service is produced by {esc(system)} \\cite{{{bibkey(system, 'SysD')}}}; its concrete interface is the AMQP IDD \\cite{{{bibkey(system, '', 'sysMonitor', 'AMQP')}}}.

The rest of this document is organized as follows.
In Section \\ref{{sec:operations}}, we describe the abstract message operations provided by the service.
In Section \\ref{{sec:model}}, we end the document by presenting the data types used by the mentioned operations.

\\subsection{{Significant Prior Art}}

{tex(svc_text(system, 'sysMonitor', 'prior'))}

\\subsection{{How This Service Is Meant to Be Used}}

{tex(svc_text(system, 'sysMonitor', 'use'))}

\\subsection{{Important Delimitations and Dependencies}}
\\label{{sec:delimitations}}

{tex(svc_text(system, 'sysMonitor', 'limits'))}

\\newpage

\\section{{Service Interface}}
\\label{{sec:operations}}

The {code('sysMonitor')} service is a publishing service: the producer publishes one observation per operation (observation type) every publication interval (\\texttt{{sysmonitor.publish.interval}}, default 30 s); consumers receive them through the broker. Each operation has no input and the output \\mref{{Observation}}.
{fig}
{rows}
\\newpage

\\section{{Information Model}}
\\label{{sec:model}}

\\msubsection{{struct}}{{Observation}}

One observation.

\\noindent\\begin{{tabularx}}{{\\textwidth}}{{| p{{4.25cm}} | p{{3.5cm}} | X |}} \\hline
\\rowcolor{{gray!33}} Field & Type & Description \\\\ \\hline
schemaVersion & \\pref{{Version}} & Version of the observation schema, \\texttt{{1.0}}. \\\\ \\hline
observationId & \\pref{{Identifier}} & Unique identifier of the observation. \\\\ \\hline
providerSystem & \\pref{{Name}} & The publishing system. \\\\ \\hline
serviceInstance & \\pref{{Name}} & The service instance (default the system name). \\\\ \\hline
observationType & \\pref{{Name}} & \\texttt{{echo}}, \\texttt{{power}} or \\texttt{{status}}. \\\\ \\hline
sourceTime & \\pref{{DateTime}} & Time of the observation. \\\\ \\hline
sequence & \\pref{{Number}} & Sequence number per publisher. \\\\ \\hline
value & \\pref{{Metadata}} & The observed value (per operation). \\\\ \\hline
unit & \\pref{{Name}} & Unit of the value, if any. \\\\ \\hline
quality & \\pref{{Name}} & \\texttt{{good}} or \\texttt{{uncertain}}. \\\\ \\hline
metadata & \\pref{{Metadata}} & \\texttt{{publisherMode=native}}. \\\\ \\hline
\\end{{tabularx}}

\\subsection{{Primitives}}
\\label{{sec:model:primitives}}

\\noindent\\begin{{tabularx}}{{\\textwidth}}{{| p{{3cm}} | X |}} \\hline
\\rowcolor{{gray!33}} Type & Description \\\\ \\hline
\\pdef{{DateTime}} & {SD_PRIMS['DateTime']} \\\\ \\hline
\\pdef{{Identifier}} & {SD_PRIMS['Identifier']} \\\\ \\hline
\\pdef{{Metadata}} & {SD_PRIMS['Metadata']} \\\\ \\hline
\\pdef{{Name}} & {SD_PRIMS['Name']} \\\\ \\hline
\\pdef{{Number}} & {SD_PRIMS['Number']} \\\\ \\hline
\\pdef{{Version}} & {SD_PRIMS['Version']} \\\\ \\hline
\\end{{tabularx}}

\\newpage
{ending(SUBJECT)}"""

def sysmonitor_idd(system):
    base = f"<Cloud>.sysmonitor.{system}.{system}"
    ex = json.dumps({"schemaVersion": "1.0", "observationId": f"{system}-echo-1792670400000-42",
                     "providerSystem": system, "serviceInstance": system, "observationType": "echo",
                     "sourceTime": "2026-10-22T12:00:00.000Z", "sequence": 42,
                     "value": {"status": "alive", "uptimeSeconds": 3600, "detail": f"{system} is running"},
                     "quality": "good", "metadata": {"publisherMode": "native"}}, indent=2)
    reg = json.dumps({"serviceDefinitionName": "sysMonitor", "version": "1.0.0",
                      "metadata": {"contract": "SysMonitor-AMQP-IDD-5.2.2", "schemaVersion": "1.0", "publisherMode": "native",
                                   "observationTypes": ["echo", "power", "status"], "delivery": "persistent, publisher-confirmed"},
                      "interfaces": [{"templateName": "generic_amqps", "protocol": "amqps", "policy": "CERT_AUTH",
                                      "properties": {"accessAddresses": ["amqp-broker"], "accessPort": 5671, "exchange": "arrowhead",
                                                     "baseRoutingKey": f"TestCloud.sysmonitor.{system}.{system}",
                                                     "operations": ["echo", "power", "status"]}}]}, indent=2)
    ops = "\n".join(f"\\osubsection{{{o}}}{{PUBLISH}}{{{code(base + '.' + o)}}}{{}}{{\\mref{{Observation}}}}\n\n{tex(t)}\n" for o, t in SYSMONITOR_OPS)
    return f"""{PRE_COMMON}{PRE_IDD}{properties("sysMonitor AMQP/TLS/JSON", "sysMonitor", "Interface Design Description", "IDD")}{front(f"This document describes the AMQP 0-9-1 protocol with TLS payload security and JSON payload encoding variant of the {code('sysMonitor')} service of the {esc(system)} system (SysMonitor-AMQP-IDD-5.2.2).", "sysMonitor")}
\\section{{Overview}}
\\label{{sec:overview}}

This document describes the AMQP binding of the native {code('sysMonitor')} service of {esc(system)}.

It's implemented using protocol, encoding as stated in below table:
\\begin{{table}}[ht!]
  \\centering
  \\begin{{tabular}}{{|l|l|l|}}
    \\hline
    \\rowcolor{{gray!33}} Profile type & Type & Version \\\\ \\hline
    Transfer protocol & AMQP & 0-9-1 \\cite{{amqp091}} \\\\ \\hline
    Data encryption & TLS & 1.3 \\cite{{rfc8446}}, 1.2 \\cite{{rfc5246}} \\\\ \\hline
    Encoding & JSON & RFC 8259 \\cite{{rfc8259}} \\\\ \\hline
    Compression & N/A & - \\\\ \\hline
    Semantics & N/A & - \\\\ \\hline
    Ontology & N/A & - \\\\ \\hline
  \\end{{tabular}}
  \\caption{{Communication and semantics details used for the {code('sysMonitor')} service interface}}
  \\label{{tab:comunication_semantics_profile}}
\\end{{table}}

This document provides the Interface Design Description IDD to the \\textit{{sysMonitor -- Service Description}} document \\cite{{{bibkey(system, '', 'sysMonitor')}}}.

\\subsection{{Interface registration}}

With \\texttt{{sysmonitor.enabled=true}}, {esc(system)} registers the service at start-up with interface template {code('generic_amqps')} (protocol {code('amqps')}, policy {code('CERT_AUTH')}; {code('generic_amqp')} without TLS) and the following properties and metadata:

\\begin{{lstlisting}}[caption={{Registered service instance (TestCloud).}}]
{reg}
\\end{{lstlisting}}

\\section{{Service Operations}}
\\label{{sec:operations}}

The publisher connects to the broker ({code('sysmonitor.broker.address')}, default {code('amqp-broker')}; port {code('sysmonitor.broker.port')}, default 5671) with TLS, verifies the broker host name and authenticates with its X.509 system certificate (SASL \\texttt{{EXTERNAL}}; the certificate CN is the broker user name). Every publication interval it publishes each observation to the topic exchange {code('arrowhead')} with the routing key \\texttt{{<baseRoutingKey>.<operation>}}, where the base routing key is \\texttt{{<Cloud>.sysmonitor.<System>.<serviceInstance>}}. The local cloud name is taken from \\texttt{{sysmonitor.local.cloud}} or the system certificate. Messages are persistent (delivery mode 2), \\texttt{{application/json}}, carry the observation identifier as message id, and are publisher-confirmed (timeout 5 s); a failed cycle is retried with a new connection at the next interval. The broker's topic permissions allow the system to publish only below its own base routing key.

{ops}
\\begin{{lstlisting}}[caption={{An \\oref{{echo}} observation.}}]
{ex}
\\end{{lstlisting}}

\\newpage

\\section{{Data Models}}
\\label{{sec:model}}

\\msubsection{{struct}}{{Observation}}

\\noindent\\begin{{tabularx}}{{\\textwidth}}{{| p{{4.25cm}} | p{{3.5cm}} | X |}} \\hline
\\rowcolor{{gray!33}} Object Field & Value Type & Description \\\\ \\hline
"schemaVersion" & \\pref{{String}} & \\texttt{{1.0}}. \\\\ \\hline
"observationId" & \\pref{{String}} & \\texttt{{<System>-<type>-<epoch ms>-<sequence>}}. \\\\ \\hline
"providerSystem" & \\pref{{String}} & The publishing system. \\\\ \\hline
"serviceInstance" & \\pref{{String}} & Service instance, default the system name. \\\\ \\hline
"observationType" & \\pref{{String}} & \\texttt{{echo}}, \\texttt{{power}} or \\texttt{{status}}. \\\\ \\hline
"sourceTime" & \\pref{{String}} & ISO 8601 UTC time with milliseconds. \\\\ \\hline
"sequence" & \\pref{{Number}} & Sequence number per publisher. \\\\ \\hline
"value" & \\pref{{Object}} & Observed value, see the operations. \\\\ \\hline
"unit" & \\pref{{String}} & \\texttt{{1}} for \\texttt{{power}} and \\texttt{{status}}; absent for \\texttt{{echo}}. \\\\ \\hline
"quality" & \\pref{{String}} & \\texttt{{good}} or \\texttt{{uncertain}}. \\\\ \\hline
"metadata" & \\pref{{Object}} & \\texttt{{\\{{"publisherMode": "native"\\}}}}. \\\\ \\hline
\\end{{tabularx}}

\\subsection{{Primitives}}
\\label{{sec:model:primitives}}

\\noindent\\begin{{tabularx}}{{\\textwidth}}{{| p{{3cm}} | X |}} \\hline
\\rowcolor{{gray!33}} Type & Description \\\\ \\hline
\\pdef{{Number}} & {JSON_PRIMS['Number']} \\\\ \\hline
\\pdef{{Object}} & {JSON_PRIMS['Object']} \\\\ \\hline
\\pdef{{String}} & {JSON_PRIMS['String']} \\\\ \\hline
\\end{{tabularx}}

\\newpage
{ending(SUBJECT)}"""

# ---------------------------------------------------------------------------------------------- SysD

def produced_rows(system, with_dd=False):
    rows = []
    for s in services_of(system):
        sd = s["serviceDefinition"]
        refs = f"\\cite{{{bibkey(system, '', sd)}}}"
        idds = f"\\cite{{{bibkey(system, '', sd, 'HTTP')}}}" + (f", \\cite{{{bibkey(system, '', sd, 'MQTT')}}}" if "mqtt" in s else "")
        rows.append((sd, s["version"], refs, idds, "HTTP(S)" + (", MQTT(S)" if "mqtt" in s else "")))
    rows.append(("sysMonitor", "1.0.0", f"\\cite{{{bibkey(system, '', 'sysMonitor')}}}", f"\\cite{{{bibkey(system, '', 'sysMonitor', 'AMQP')}}}", "AMQP(S)"))
    return rows

def consumed_ref(provider, service):
    first = service.split(",")[0].split(";")[0].strip()
    if provider in SYSTEMS and any(first == s["serviceDefinition"] for s in MODEL["systems"][provider]["services"]):
        return f"\\cite{{{bibkey(provider, '', first)}}}"
    return "--"

def sysd_doc(system):
    S = SYSTEMS[system]
    prod = produced_rows(system)
    prod_tab = "\n".join(f"{code(sd)} & {esc(v)} & {ifs} & {r} & {i} \\\\ \\hline" for sd, v, r, i, ifs in prod)
    cons_tab = "\n".join(f"{tex(p)} & {tex(s)} & {tex(o)} & {tex(u)} & {tex(c)} \\\\ \\hline" for p, s, o, u, c in S["consumed"])
    func = "\n".join(f"  \\item {tex(x)}" for x in S["functional"])
    cfg = "\n".join(f"{code(k) if '/' not in k else ' / '.join(code(x.strip()) for x in k.split('/'))} & {tex(v)} & {tex(m)} \\\\ \\hline" for k, v, m in S["config"])
    fig = system_figure(system, [r[0] for r in prod], sorted({p for p, *_ in S["consumed"]}))
    abstract = f"This document describes the {esc(system)} core system of the Eclipse Arrowhead {VERSION} framework, as implemented in Java Spring in \\texttt{{ah5-core-java-spring/{esc(S['module'])}}}, including the local extensions of this workspace."
    return f"""{PRE_COMMON}{PRE_SYS}{properties(esc(system), None, "System Description", "SysD")}{front(abstract)}
\\section{{Overview}}
\\label{{sec:overview}}

This document describes the {esc(system)} system. {tex(S['overview'])}
{fig}
The rest of this document is organized as follows.
In Section \\ref{{sec:prior_art}}, we reference major prior art capabilities of the system.
In Section \\ref{{sec:use}}, we describe the intended usage of the system.
In Section \\ref{{sec:properties}}, we describe fundamental properties provided by the system.
In Section \\ref{{sec:delimitations}}, we describe delimitations of capabilities of the system.
In Section \\ref{{sec:services}}, we describe the abstract service functions consumed or produced by the system.
In Section \\ref{{sec:security}}, we describe the security capabilities of the system.

\\newpage

\\subsection{{Significant Prior Art}}
\\label{{sec:prior_art}}

{tex(S['prior'])}

\\subsection{{How This System Is Meant to Be Used}}
\\label{{sec:use}}

{tex(S['use'])}

\\subsection{{System functionalities and properties}}
\\label{{sec:properties}}

\\subsubsection{{Functional properties of the system}}

\\begin{{itemize}}
{func}
\\end{{itemize}}

\\subsubsection{{Configuration of system properties}}

The system is configured with Spring properties (\\texttt{{application.properties}}, overridden by environment variables in the container deployment). The properties that shape its behaviour are:

\\noindent\\begin{{longtable}}{{| p{{5.4cm}} | p{{3.4cm}} | p{{6.4cm}} |}} \\hline
\\rowcolor{{gray!33}} Property & Default & Meaning \\\\ \\hline \\endhead
{cfg}
\\end{{longtable}}

Secrets (database, keystore and identity passwords, secret keys) have no usable defaults; in the test clouds they are per-cloud secrets mounted as files.

\\subsubsection{{Data stored by the system}}

{tex(S['data'])}

\\subsubsection{{Non functional properties}}

{tex(S['nonfunc'])}

\\subsubsection{{Stateful or stateless}}

{tex(S['state'])}

\\subsection{{Important Delimitations}}
\\label{{sec:delimitations}}

{tex(S['limits'])}

\\newpage

\\section{{Services}}
\\label{{sec:services}}

This section describes the produced and consumed services. Each produced service is specified by an SD document and one IDD document per interface.

\\subsection{{Produced services}}

\\noindent\\begin{{longtable}}{{| p{{5.3cm}} | p{{1.3cm}} | p{{2.6cm}} | p{{1.6cm}} | p{{3.6cm}} |}} \\hline
\\rowcolor{{gray!33}} Service & Version & Interfaces & SD & IDD \\\\ \\hline \\endhead
{prod_tab}
\\end{{longtable}}

The HTTP interfaces are always offered, the MQTT interfaces with \\texttt{{mqtt.api.enabled=true}} and the AMQP \\texttt{{sysMonitor}} interface with \\texttt{{sysmonitor.enabled=true}}. Upstream 5.2.2 also answers \\texttt{{GET <base>/monitor/echo}} on some systems; this endpoint is not registered as a service.

\\subsection{{Consumed services}}

\\noindent\\begin{{longtable}}{{| >{{\\raggedright\\arraybackslash}}p{{2.9cm}} | >{{\\raggedright\\arraybackslash}}p{{3.1cm}} | >{{\\raggedright\\arraybackslash}}p{{2.5cm}} | p{{3.6cm}} | >{{\\raggedright\\arraybackslash}}p{{2.9cm}} |}} \\hline
\\rowcolor{{gray!33}} Provider & Service & Operations & Use & Condition \\\\ \\hline \\endhead
{cons_tab}
\\end{{longtable}}

Except for the bootstrap services (\\texttt{{http.collector.bootstrap.services}}), which are looked up in the ServiceRegistry, the system reaches other systems' services through DynamicServiceOrchestration (\\texttt{{http.collector.mode=ORCHESTRATION}}, results cached for \\texttt{{http.collector.orchestration.ttl}} = 300 s); every such call needs a ConsumerAuthorization grant.

\\newpage

\\section{{Security}}
\\label{{sec:security}}

The default is the Arrowhead secure mode with HTTPS and X.509 certificates following the Arrowhead certificate profile (\\texttt{{<System>.<Cloud>.<Organization>.arrowhead.eu}}). Certificates not issued under the cloud CA are refused by the TLS handshake. The Arrowhead-test-cloud-java deployment runs the secure mode.

The packaged \\texttt{{application.properties}} is a development sample that disables TLS and declares identities. The system refuses to start with it, or with any other effective configuration that sets \\texttt{{server.ssl.enabled=false}}, \\texttt{{authentication.policy=declared}}, \\texttt{{service.authorization.enabled=false}} or, while monitoring is on, \\texttt{{sysmonitor.ssl.enabled=false}}. The check runs before the system accepts a connection. Only an explicit \\texttt{{arrowhead.security.profile=development}} lets such a system start, for an isolated test, and it then logs a non-conformance warning.

\\subsection{{Security model}}

\\begin{{itemize}}
  \\item \\textbf{{Protocols:}} HTTPS (TLS 1.3 and 1.2) with mandatory client certificates (\\texttt{{server.ssl.client-auth=need}}); MQTT over TLS; AMQP over TLS for \\texttt{{sysMonitor}}.
  \\item \\textbf{{Data protection:}} transport encryption by TLS; the database connection uses TLS with server verification in the test cloud. Secrets are kept out of the container environment (Compose file secrets).
  \\item \\textbf{{System authentication:}} by the authentication policy: \\texttt{{certificate}} (client certificate CN), \\texttt{{outsourced}} (identity token verified with Authentication), \\texttt{{internal}} (Authentication only) or \\texttt{{declared}} (insecure). In this workspace an identity token is accepted only together with the client certificate of the system it was issued to (token-to-certificate binding), and every client certificate is checked against the cloud CA's CRL when \\texttt{{arrowhead.ssl.crl-file}} is set (fail closed).
  \\item \\textbf{{Authorization of produced services:}} management services are protected by the management filter (\\texttt{{management.policy}}: \\texttt{{SYSOP\\_ONLY}}, \\texttt{{WHITELIST}} or \\texttt{{AUTHORIZATION}} via ConsumerAuthorization); an optional blacklist filter refuses blacklisted requesters. Sysop identities bypass the management filter.
  \\item {tex(S['security_extra'])}
\\end{{itemize}}

For the Arrowhead certificate profile see github.com/eclipse-arrowhead/documentation.

{ending(SUBJECT)}"""

# ---------------------------------------------------------------------------------------------- SysDD

def sysdd_doc(system):
    S = SYSTEMS[system]
    comps = "\n".join(f"{code(k)} & {tex(v)} \\\\ \\hline" for k, v in S["components"])
    tabs = "\n".join(f"{code(k)} & {tex(v)} \\\\ \\hline" for k, v in S["tables"])
    local = "\n".join(f"  \\item {tex(x)}" for x in S["local"])
    svc_rows = []
    for sd, v, r, i, ifs in produced_rows(system):
        svc_rows.append(f"{code(sd)} & \\cite{{{bibkey(system, 'SysD')}}} & {r} & {i} \\\\ \\hline")
    cons_rows = []
    for p, s, o, u, c in S["consumed"]:
        sref = f"\\cite{{{bibkey(p, 'SysD')}}}" if p in SYSTEMS else tex(p)
        cons_rows.append(f"{tex(s)} & {sref} & {consumed_ref(p, s)} & -- \\\\ \\hline")
    dtos = sorted({d for s in services_of(system) for o in s.get("http", {}).get("ops", []) for d in dto_closure(list(op_io(o)))}, key=strip_dto)
    tests, runs = S["tests"][0], S["tests"][1]
    abstract = f"This document describes the design and implementation of the {esc(system)} core system of Arrowhead {VERSION} in Java Spring (\\texttt{{ah5-core-java-spring/{esc(S['module'])}}})."
    return f"""{PRE_COMMON}{PRE_SYS}{properties(esc(system), None, "System Design Description", "SysDD")}{front(abstract)}
\\section{{Overview}}
\\label{{sec:overview}}

This document describes the {esc(system)} system implementation, which realizes the {esc(system)} SysD \\cite{{{bibkey(system, 'SysD')}}}. {tex(S['overview'])}
In Section \\ref{{Implementation}}, we describe implementation details of the system.

\\newpage

\\section{{Implementation}}
\\label{{Implementation}}

This implementation is based on the SysD document {esc(system)} \\cite{{{bibkey(system, 'SysD')}}}, the upstream Eclipse Arrowhead Java Spring implementation 5.2.2 \\cite{{ah5java}} and the local changes listed in Section \\ref{{sec:local}}.

\\subsection{{Implementation language and tools}}
\\begin{{itemize}}
\\item Java 21, Spring Boot 3.4.9 (web, security, data JPA, Quartz), springdoc-openapi 2.8 (Swagger UI), Log4j 2.
\\item Maven module \\texttt{{{esc(S['module'])}}} of \\texttt{{ah5-core-java-spring}}; it depends on \\texttt{{arrowhead-common-utils}} and \\texttt{{arrowhead-data-transfer-objects}} 5.2.2 from the local \\texttt{{ah5-common-java-spring}} (Eclipse Paho MQTT v3 1.2.5, RabbitMQ AMQP client 5.22.0).
\\item Database: MariaDB/MySQL (MariaDB Connector/J; MySQL 8.4 server in the test cloud), schema validated by Hibernate (\\texttt{{ddl-auto=validate}}); SQL scripts in \\texttt{{src/main/resources/database}}.
\\item Stateless request processing; all state is in the database. Scheduled jobs use Quartz.
\\end{{itemize}}

\\subsection{{Functional properties implementation}}

The Java packages below \\texttt{{eu.arrowhead}} implement the system:

\\noindent\\begin{{longtable}}{{| p{{5.6cm}} | p{{9.6cm}} |}} \\hline
\\rowcolor{{gray!33}} Package & Responsibility \\\\ \\hline \\endhead
{comps}
\\end{{longtable}}

Every request passes the common filter chain: authentication filter (by \\texttt{{authentication.policy}}), optional blacklist filter, management service filter (for management services), then the controller, validation and normalization, the service logic and the database service. The same logic is reachable through the MQTT topic handlers.

\\subsubsection{{Data handled and stored}}

Database \\texttt{{{esc(S['db'])}}}:

\\noindent\\begin{{longtable}}{{| p{{6.4cm}} | p{{8.8cm}} |}} \\hline
\\rowcolor{{gray!33}} Tables & Content \\\\ \\hline \\endhead
{tabs}
\\end{{longtable}}

The data models of the interfaces are the Java records of \\texttt{{eu.arrowhead.dto}} (common library); the IDD documents name them without the \\texttt{{DTO}} suffix. The records used by this system are: {", ".join(code(d) for d in dtos)}.

\\subsection{{Non functional properties implementation}}

\\subsubsection{{Security}}

TLS and client authentication are configured with the Spring \\texttt{{server.ssl.*}} properties (PKCS\\#12 keystore and truststore). The authentication filters of the common library implement the four authentication policies; \\texttt{{TokenCertificateBinding}} binds identity tokens to the client certificate and \\texttt{{CrlRevocationConfig}} enables CRL checking in Tomcat (both local). The private key of the keystore is also used to sign the system's outgoing requests where required.

\\subsubsection{{Power management}}

None; the system is a server process without power states. The \\texttt{{sysMonitor}} \\texttt{{power}} observation always reports line supply.

\\subsubsection{{Internal monitoring}}

Log4j 2 logs to stdout and rolling files; log entries are also written to the \\texttt{{logs}} table and served by \\texttt{{generalManagement.get-log}}. The native \\texttt{{sysMonitor}} publisher (\\texttt{{eu.arrowhead.common.sysmonitor}}) publishes \\texttt{{echo}}, \\texttt{{power}} and \\texttt{{status}} every 30 s.

\\subsubsection{{Configuration}}

Configuration is read from \\texttt{{application.properties}} and environment variables (Spring relaxed binding, e.g. \\texttt{{SERVICE\\_DISCOVERY\\_POLICY}}); secrets come from files through \\texttt{{SPRING\\_CONFIG\\_IMPORT=configtree:/run/secrets/}}. The properties are listed in the SysD. \\texttt{{generalManagement.get-config}} returns the public ones.

\\subsection{{Deployment}}

The test cloud (\\texttt{{Arrowhead-test-cloud-java}}) builds the image {code(S['image'].split(':')[0].split('/')[-1] + ':5.2.2-sysmonitor')} from the released image {code(S['image'])} with the locally built {code(S['jar'])} (\\texttt{{java-patch/Dockerfile}}). The container runs read-only with the configuration baked into \\texttt{{/app/config}}, only its keystores mounted, and publishes port {S['port']} on the loopback address of the host. Its database runs in a separate container.

\\subsection{{Local modifications}}
\\label{{sec:local}}

Compared to upstream 5.2.2, this workspace changes:
\\begin{{itemize}}
{local}
\\end{{itemize}}

\\subsection{{Verification}}

The module has {tests} JUnit test classes; the regression report set recorded on {DATE if system == "DynamicServiceOrchestration" else "2026-10-20"} covers {runs} tests without failures. Live-cloud smoke tests are a separate verification step; the plant-management migration is not claimed as deployed by this update.

\\newpage

\\section{{Services}}
\\label{{sec:services}}

The implementation of the services is based on the SD and IDD documents referenced below.

\\noindent\\begin{{longtable}}{{| p{{5.4cm}} | p{{2.2cm}} | p{{2.2cm}} | p{{4.8cm}} |}} \\hline
\\rowcolor{{gray!33}}\\textbf{{Services produced}} & \\textbf{{SysD ref}} & \\textbf{{SD ref}} & \\textbf{{IDD ref}} \\\\ \\hline \\endhead
{chr(10).join(svc_rows)}
&&& \\\\ \\hline
\\rowcolor{{gray!33}}\\textbf{{Services consumed}} & \\textbf{{SysD ref}} & \\textbf{{SD ref}} & \\textbf{{IDD ref}} \\\\ \\hline
{chr(10).join(cons_rows)}
\\end{{longtable}}

{ending(SUBJECT)}"""

# ---------------------------------------------------------------------------------------------- bibliography

def bibliography():
    out = []
    for system in ORDER:
        S = SYSTEMS[system]
        out.append(f"@techreport{{{bibkey(system, 'SysD')}, author={{{AUTHOR}}}, title={{{{{system} -- System Description}}}}, institution={{Eclipse Arrowhead}}, type={{SysD}}, number={{{VERSION}}}, year={{2026}}, note={{{S['module']}/documentation/v{VERSION}/{docname(system, 'SysD')}.pdf}}}}")
        out.append(f"@techreport{{{bibkey(system, 'SysDD')}, author={{{AUTHOR}}}, title={{{{{system} -- System Design Description}}}}, institution={{Eclipse Arrowhead}}, type={{SysDD}}, number={{{VERSION}}}, year={{2026}}, note={{{S['module']}/documentation/v{VERSION}/{docname(system, 'SysDD')}.pdf}}}}")
        for s in MODEL["systems"][system]["services"] + [{"serviceDefinition": "sysMonitor", "amqp": True}]:
            sd = s["serviceDefinition"]
            out.append(f"@techreport{{{bibkey(system, '', sd)}, author={{{AUTHOR}}}, title={{{{{sd} -- Service Description ({system})}}}}, institution={{Eclipse Arrowhead}}, type={{SD}}, number={{{VERSION}}}, year={{2026}}, note={{{docname(system, '', sd)}.pdf}}}}")
            for b in (["AMQP"] if s.get("amqp") else ["HTTP"] + (["MQTT"] if "mqtt" in s else [])):
                proto = {"HTTP": "HTTP/TLS/JSON", "MQTT": "MQTT/TLS/JSON", "AMQP": "AMQP/TLS/JSON"}[b]
                out.append(f"@techreport{{{bibkey(system, '', sd, b)}, author={{{AUTHOR}}}, title={{{{{sd} {proto} -- Interface Design Description ({system})}}}}, institution={{Eclipse Arrowhead}}, type={{IDD}}, number={{{VERSION}}}, year={{2026}}, note={{{docname(system, '', sd, b)}.pdf}}}}")
    out += [
        "@misc{ah5java, author={{AITIA International}}, title={Eclipse Arrowhead 5 Core Systems (Java Spring) and documentation}, howpublished={\\url{https://aitia-iiot.github.io/ah5-docs-java-spring/}}, year={2025}}",
        "@techreport{rfc8259, author={T. Bray}, title={The JavaScript Object Notation (JSON) Data Interchange Format}, institution={IETF}, type={RFC}, number={8259}, year={2017}}",
        "@techreport{rfc9112, author={R. Fielding and M. Nottingham and J. Reschke}, title={HTTP/1.1}, institution={IETF}, type={RFC}, number={9112}, year={2022}}",
        "@techreport{rfc8446, author={E. Rescorla}, title={The Transport Layer Security (TLS) Protocol Version 1.3}, institution={IETF}, type={RFC}, number={8446}, year={2018}}",
        "@techreport{rfc5246, author={T. Dierks and E. Rescorla}, title={The Transport Layer Security (TLS) Protocol Version 1.2}, institution={IETF}, type={RFC}, number={5246}, year={2008}}",
        "@techreport{mqtt311, author={A. Banks and R. Gupta}, title={MQTT Version 3.1.1}, institution={OASIS}, type={Standard}, year={2014}}",
        "@techreport{amqp091, author={{AMQP Working Group}}, title={AMQP 0-9-1 Specification}, institution={AMQP Working Group}, year={2008}}",
    ]
    return "\n\n".join(out) + "\n"

# ---------------------------------------------------------------------------------------------- package files

README = """# {system} {version} documentation

Arrowhead LaTeX documentation of the {system} core system (Java Spring, `ah5-core-java-spring/{module}`),
written with the Eclipse Arrowhead templates (`templates/`, from `roadmap/Documentation_templates/LaTeX`).

## System documents

- `{system}-v{version}-SysD.tex` — System Description (black box).
- `{system}-v{version}-SysDD.tex` — System Design Description (implementation, deployment).

## Service descriptions and interface design descriptions

| Service | SD | IDD |
|---|---|---|
{rows}

PDFs are built beside the sources with `./build-docs.sh` (latexmk, pdfLaTeX and BibTeX). See `SOURCES.md` for
what the content was checked against.
"""

SOURCES = """# Sources

Structure, cover metadata and macros follow the Eclipse Arrowhead LaTeX templates
(`roadmap/Documentation_templates/LaTeX/template_{{sysd,sysdd,sd,idd}}.tex`, copied to `templates/`); `arrowhead.cls`
and the logo are copied from there. The SD and IDD `\\osubsection` macros take their input and output arguments
already formatted, so that primitive types can be named as well as structs.

The service, operation, path, parameter and data-model content was extracted from the Java sources:

- `ah5-core-java-spring/{module}/src/main/java` — `*SystemInfo.java` (registered services, operations,
  interfaces), `api/http/*API.java` (paths, methods, parameters, bodies, status codes), `api/mqtt/*Handler.java`
  (MQTT topics and payloads), `src/main/resources/application.properties` and `database/create_tables.sql`.
- `ah5-common-java-spring/data-transfer-objects` (DTO records and enums) and `common-utils` (filters, sysMonitor
  publisher, token-to-certificate binding, CRL checking).
- `Arrowhead-test-cloud-java/compose.yaml` and `java-patch/Dockerfile` (deployment).

Descriptions of fields and operations were written for these documents from the implementation; they do not copy the
upstream documentation site. The documents describe the implementation of this workspace, including its local
changes, and are marked RELEASE CANDIDATE until quality assurance.

These files are generated by `ah5-core-java-spring/documentation-tools` (`regenerate.sh`); edit the generator, not
the `.tex` files.
"""

BUILD = """#!/bin/sh
# Builds the PDFs of this directory (latexmk with pdfLaTeX and BibTeX) and removes the auxiliary files.
set -eu
cd "$(dirname "$0")"
for source in ./*.tex; do
  latexmk -pdf -interaction=nonstopmode -halt-on-error -quiet "$source"
done
latexmk -c -quiet
rm -f ./*.bbl
echo "{system} documentation PDFs are current."
"""

GITIGNORE = "*.aux\n*.bbl\n*.blg\n*.fdb_latexmk\n*.fls\n*.log\n*.out\n*.toc\n*.lot\n*.lof\n"

def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)

def main():
    count = 0
    bib = bibliography()
    for system in ORDER:
        S = SYSTEMS[system]
        out = CORE / S["module"] / "documentation" / f"v{VERSION}"
        out.mkdir(parents=True, exist_ok=True)
        (out / "figures").mkdir(exist_ok=True)
        (out / "templates").mkdir(exist_ok=True)
        shutil.copy(TEMPLATES / "arrowhead.cls", out / "arrowhead.cls")
        shutil.copy(TEMPLATES / "IEEEtran.bst", out / "IEEEtran.bst")
        shutil.copy(TEMPLATES / "figures/arrowhead_logo.pdf", out / "figures/arrowhead_logo.pdf")
        for t in ("template_sysd.tex", "template_sysdd.tex", "template_sd.tex", "template_idd.tex"):
            shutil.copy(TEMPLATES / t, out / "templates" / t)
        write(out / "bibliography.bib", bib)
        write(out / f"{docname(system, 'SysD')}.tex", sysd_doc(system)); count += 1
        write(out / f"{docname(system, 'SysDD')}.tex", sysdd_doc(system)); count += 1
        rows = []
        for s in services_of(system):
            sd = s["serviceDefinition"]
            write(out / f"{docname(system, '', sd)}.tex", sd_doc(system, s)); count += 1
            write(out / f"{docname(system, '', sd, 'HTTP')}.tex", http_idd(system, s)); count += 1
            idds = [f"`{docname(system, '', sd, 'HTTP')}.tex`"]
            if "mqtt" in s:
                write(out / f"{docname(system, '', sd, 'MQTT')}.tex", mqtt_idd(system, s)); count += 1
                idds.append(f"`{docname(system, '', sd, 'MQTT')}.tex`")
            rows.append(f"| `{sd}` | `{docname(system, '', sd)}.tex` | {', '.join(idds)} |")
        write(out / f"{docname(system, '', 'sysMonitor')}.tex", sysmonitor_sd(system)); count += 1
        write(out / f"{docname(system, '', 'sysMonitor', 'AMQP')}.tex", sysmonitor_idd(system)); count += 1
        rows.append(f"| `sysMonitor` | `{docname(system, '', 'sysMonitor')}.tex` | `{docname(system, '', 'sysMonitor', 'AMQP')}.tex` |")
        write(out / "README.md", README.format(system=system, version=VERSION, module=S["module"], rows="\n".join(rows)))
        write(out / "SOURCES.md", SOURCES.format(module=S["module"]))
        write(out / "build-docs.sh", BUILD.format(system=system))
        (out / "build-docs.sh").chmod(0o755)
        write(out / ".gitignore", GITIGNORE)
    print(f"{count} documents written")

main()
