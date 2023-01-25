package org.meveo.stripe;


import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;


import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.StrCheckoutInfo;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.exception.StripeException;


public class StripeCheckoutSuccess extends Script {
	
    private static final Logger Log = LoggerFactory.getLogger(StripeCheckoutSuccess.class);
  
    @Inject
	private RepositoryService repositoryService;
  
    @Inject
    private CrossStorageApi crossStorageApi;
  
	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		super.execute(parameters);
      
        Stripe.apiKey = "sk_test_51ME7KzF8O6FLWQWJwzBsPG7XXyr1uVSjsRF7J1OkLvusWPUi3aehz6xntJHirHqVdjsdadTHbRF5w9atu3b9QhPk002fWXABem";
        Log.info("===============================================================");
        //.get("metaData").get("checkoutInfoId")
        Log.info("data = "+parameters.get("data").toString());
        Log.info("Called from stripe.com");
        String checkOutInfoId = this.extractCheckoutInfoId(parameters.get("data").toString());
        Log.info("checkOutInfoId="+checkOutInfoId);
        Log.info("===============================================================");
      
        Repository defaultRepo = repositoryService.findDefaultRepository();
		try {
            StrCheckoutInfo checkoutInfo = crossStorageApi.find(defaultRepo, checkOutInfoId, StrCheckoutInfo.class);
            checkoutInfo.setResponseCode("200");
            checkoutInfo.setResponse(parameters.get("data").toString());
			crossStorageApi.createOrUpdate(defaultRepo, checkoutInfo);            
			Log.info("checkoutInfo instance {} updated", checkOutInfoId);
		} catch (Exception ex) {
			throw new BusinessException(ex);
		}
      
	}
  
    private String extractCheckoutInfoId(String paramInfo){
        String info =paramInfo;
        info = info.substring(info.indexOf("checkoutInfoId")+15);
        String checkoutInfoId = info.substring(0,info.indexOf("}"));
        return   checkoutInfoId;
    }
	
}