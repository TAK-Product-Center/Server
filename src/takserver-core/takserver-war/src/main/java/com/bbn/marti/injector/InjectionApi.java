package com.bbn.marti.injector;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.antlr.v4.parse.GrammarTreeVisitor.locals_return;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stringtemplate.v4.compiler.CodeGenerator.includeExpr_return;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.injector.InjectorConfig;
import com.bbn.marti.remote.service.InjectionService;
import com.google.common.base.Strings;

import tak.server.Constants;

/*
 * 
 * REST API for message injectors
 */
@RestController
public class InjectionApi extends BaseRestController {
	private static final Logger logger = LoggerFactory.getLogger(InjectionApi.class);

    private final Pattern badUid = Pattern.compile(".*[<>'\"].*");
    private final Pattern badInject = Pattern.compile(".*[<>].*");
    
    public static final String BASE_PATH = "/injectors/cot/uid";
    
    @Autowired
    InjectionService injectionService;
    
    /*
     * GET all CoT uid injectors
     */
    @RequestMapping(value = BASE_PATH, method = RequestMethod.GET)
    ApiResponse<Set<InjectorConfig>> getAllCotInjectors() {

        return new ApiResponse<Set<InjectorConfig>>(Constants.API_VERSION, InjectorConfig.class.getSimpleName(), injectionService.getAllInjectors());
    }
    
    /*
     * GET one CoT uid injector
     */
    @RequestMapping(value = BASE_PATH + "/{uid}", method = RequestMethod.GET)
    ApiResponse<Collection<InjectorConfig>> getOneCotInjector(@PathVariable("uid") @NotNull String uid) {

        if (Strings.isNullOrEmpty(uid)) {
		    throw new IllegalArgumentException("empty uid");
		}

		Collection<InjectorConfig> injectors = injectionService.getInjectors(uid);

		return new ApiResponse<Collection<InjectorConfig>>(Constants.API_VERSION, InjectorConfig.class.getSimpleName(), injectors);
    }
    
    /*
     * Upsert one CoT uid injector
     * 
     */
    @RequestMapping(value = BASE_PATH, method = RequestMethod.POST)
    ApiResponse<Set<InjectorConfig>> putCotInjector(@RequestBody InjectorConfig injector) {
        
        
        if (Strings.isNullOrEmpty(injector.getUid())) {
            throw new IllegalArgumentException("empty uid");
        }
        
        if (Strings.isNullOrEmpty(injector.getToInject())) {
            throw new IllegalArgumentException("empty toInject");
        }
        
        if (badUid.matcher(injector.getUid()).matches()) {
            throw new IllegalArgumentException("invalid uid");
        }
        
        
        if (badInject.matcher(injector.getToInject()).matches()) {
            throw new IllegalArgumentException("invalid toInject");
        }
        
        injectionService.setInjector(injector.getUid(), injector.getToInject());
         
		Set<InjectorConfig> result = new HashSet<>();
		
		result.add(injector);
		
		return new ApiResponse<Set<InjectorConfig>>(Constants.API_VERSION, InjectorConfig.class.getSimpleName(), result);
    }
    
    /*
     * delete an injector by name. Respond with the deleted injector JSON.
     * 
     */
    @RequestMapping(value = BASE_PATH, method = RequestMethod.DELETE)
    ApiResponse<Set<InjectorConfig>> deleteInjector(@RequestParam(name = "uid") String uid, @RequestParam(name = "toInject") String toInject) {

        try {
            Set<InjectorConfig> result = new HashSet<>();
            InjectorConfig ic = new InjectorConfig();
            
            ic.setToInject(toInject);
            ic.setUid(uid);
            
            ic = injectionService.deleteInjector(ic);
            
            if (ic != null) {
                result.add(ic);
            }
            
            return new ApiResponse<Set<InjectorConfig>>(Constants.API_VERSION, InjectorConfig.class.getSimpleName(), result);

        } catch (RemoteException e) {
            throw new TakException(e);
        }
    }    
}
