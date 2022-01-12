package es.fhir.rest.core.servlets;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.web.servlet.IniShiroFilter;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import es.fhir.rest.core.resources.IFhirResourceProvider;
import info.elexis.server.core.SystemPropertyConstants;

@Component(service = CoreFhirRestServlet.class, immediate = true)
public class CoreFhirRestServlet extends RestfulServer {
	
	private static final String FHIR_BASE_URL = "/fhir";
	
	private static Logger logger = LoggerFactory.getLogger(CoreFhirRestServlet.class);
	
	private static final long serialVersionUID = -4760702567124041329L;
	
	@Reference
	private HttpService httpService;
	
	// resource providers
	private List<IFhirResourceProvider> providers;
	
	@Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	public synchronized void bindFhirProvider(IFhirResourceProvider provider){
		if (providers == null) {
			providers = new ArrayList<IFhirResourceProvider>();
		}
		providers.add(provider);
		registerProvider(provider);
	}
	
	public void unbindFhirProvider(IFhirResourceProvider provider){
		if (providers == null) {
			providers = new ArrayList<IFhirResourceProvider>();
		}
		providers.remove(provider);
		try {
			unregisterProvider(provider);
		} catch (Exception e) {
			// ignore
		}
	}
	
	public CoreFhirRestServlet(){
		super(FhirContext.forR4());
		setServerName("Elexis-Server FHIR");
		setServerVersion("3.9");
//		setServerConformanceProvider(new ServerCapabilityStatementProvider(this));
	}
	
	@Activate
	public void activate(){
		Thread.currentThread().setContextClassLoader(CoreFhirRestServlet.class.getClassLoader());
		ExtendedHttpService extHttpService = (ExtendedHttpService) httpService;
		try {
			String shiroConfig =
				(SystemPropertyConstants.isDisableWebSecurity()) ? "shiro-fhir-nosec.ini"
						: "shiro-fhir.ini";
			httpService.registerServlet(FHIR_BASE_URL + "/*", this, null, null);
			String config = IOUtils.toString(this.getClass().getResourceAsStream(shiroConfig),
				Charset.forName("UTF-8"));
			IniShiroFilter iniShiroFilter = new IniShiroFilter();
			iniShiroFilter.setConfig(config);
			extHttpService.registerFilter(FHIR_BASE_URL, iniShiroFilter, null, null);
		} catch (ServletException | NamespaceException | IOException e) {
			logger.error("Could not register FHIR servlet.", e);
		}
	}
	
	@Deactivate
	public void deactivate(){
		logger.debug("Deactivating CoreFhirRestServlet");
		httpService.unregister(FHIR_BASE_URL + "/*");
	}
	
	/**
	 * The initialize method is automatically called when the servlet is starting up, so it can be
	 * used to configure the servlet to define resource providers, or set up configuration,
	 * interceptors, etc.
	 */
	@Override
	protected void initialize() throws ServletException{
		/*
		 *  This interceptor is used to generate a new log line (via SLF4j) for each incoming request.
		 */
		LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
		registerInterceptor(loggingInterceptor);
		loggingInterceptor.setMessageFormat("REQ ${requestHeader.user-agent}@${remoteAddr} ${operationType} ${idOrResourceName} ${requestParameters}");
		loggingInterceptor.setErrorMessageFormat("REQ_ERR ${requestHeader.user-agent}@${remoteAddr} ${operationType} ${idOrResourceName} ${requestParameters} - ${exceptionMessage}");
		
		/*
		 * This server interceptor causes the server to return nicely formatter and
		 * coloured responses instead of plain JSON/XML if the request is coming from a
		 * browser window. It is optional, but can be nice for testing.
		 */
		registerInterceptor(new ResponseHighlighterInterceptor());
		
		/*
		 * Tells the server to return pretty-printed responses by default
		 */
		setDefaultPrettyPrint(true);
	}
}
