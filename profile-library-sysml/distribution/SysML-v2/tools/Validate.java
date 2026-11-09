import java.nio.file.*;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.validation.*;
import org.eclipse.xtext.util.CancelIndicator;
import org.omg.sysml.xtext.SysMLStandaloneSetup;
public class Validate {
 public static void main(String[] args) throws Exception {
  org.eclipse.emf.ecore.EPackage.Registry.INSTANCE.put("https://www.omg.org/spec/SysML/20250201", org.omg.sysml.lang.sysml.SysMLPackage.eINSTANCE);
  org.eclipse.emf.ecore.EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.put("http://www.omg.org/spec/SysML", new org.omg.sysml.delegate.setting.DerivedPropertySettingDelegateFactory());
  org.eclipse.emf.ecore.EOperation.Internal.InvocationDelegate.Factory.Registry.INSTANCE.put("http://www.omg.org/spec/SysML", new org.omg.sysml.delegate.invocation.OperationInvocationDelegateFactory());
  new org.omg.kerml.xtext.KerMLStandaloneSetup().createInjectorAndDoEMFRegistration();
  var injector = new SysMLStandaloneSetup().createInjectorAndDoEMFRegistration();
  var rs = injector.getInstance(XtextResourceSet.class);
  for (String root : args) try(var paths=Files.walk(Path.of(root))) {
   for(var p:paths.filter(p->p.toString().endsWith(".sysml")||p.toString().endsWith(".kerml")).sorted().toList()) rs.getResource(URI.createFileURI(p.toAbsolutePath().toString()),true);
  }
  System.out.println("LOADED "+rs.getResources().size());
  if (System.getenv("SYNTAX_ONLY") != null) { int n=0; for(Resource r:rs.getResources()) for(var e:r.getErrors()) { System.out.println(r.getURI().lastSegment()+":"+e.getLine()+" "+e.getMessage());n++; } System.out.println("SYNTAX_ERRORS="+n); if(n>0) System.exit(1); return; }
  EcoreUtil.resolveAll(rs);
  var validator=injector.getInstance(IResourceValidator.class); int errors=0,warnings=0;
  for(Resource r:rs.getResources()) if(r.getURI().isFile() && Path.of(r.getURI().toFileString()).startsWith(Path.of(args[args.length-1]).toAbsolutePath())) {
   for(var issue:validator.validate(r,CheckMode.ALL,CancelIndicator.NullImpl)) {
    if(issue.getSeverity()==org.eclipse.xtext.diagnostics.Severity.ERROR) errors++; else warnings++;
    System.out.println(r.getURI().lastSegment()+":"+issue.getLineNumber()+" "+issue.getSeverity()+" "+issue.getMessage());
   }
  }
  System.out.println("ERRORS="+errors+" WARNINGS="+warnings); if(errors>0) System.exit(1);
 }
}
