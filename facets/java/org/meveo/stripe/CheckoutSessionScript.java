package org.meveo.stripe;

import javax.inject.Inject;
import java.util.Map;
import java.time.Instant;
import java.util.HashMap;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

//import org.meveo.service.admin.impl.credentials.CredentialHelperService;
//import org.meveo.model.admin.MvCredential;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.meveo.model.customEntities.StrCheckoutInfo;

import org.meveo.api.rest.technicalservice.EndpointScript;

public class CheckoutSessionScript extends EndpointScript {

	@Inject
	private CrossStorageApi crossStorageApi;

	@Inject
	private RepositoryService repositoryService;

	//@Inject
	//private CredentialHelperService credentialHelperService;

	private static final Logger Log = LoggerFactory.getLogger(CheckoutSessionScript.class);

	private String responseUrl="";

	public String getResponseUrl() {
		return responseUrl;
	}

	@Override
	public void execute(Map<String, Object> parameters) throws BusinessException {
		Log.info("received {}", parameters);
		StrCheckoutInfo checkoutInfo = new StrCheckoutInfo();
		checkoutInfo.setCreationDate(Instant.now());

		// as long as we have an email we process the payment
		if (parameters.containsKey("email")) {
			checkoutInfo.setEmail(parameters.get("email").toString());
		} else {
			endpointResponse.setStatus(400);
			endpointResponse.setErrorMessage("missing email");
		}

		Map<String, String> inputInfo = new HashMap<>();
		if (parameters.containsKey("tpk_id")) {
			inputInfo.put("tpk_id", parameters.get("tpk_id").toString());
		}
		if (parameters.containsKey("value")) {
			inputInfo.put("value", parameters.get("value").toString());
		}
		if (parameters.containsKey("token")) {
			inputInfo.put("token", parameters.get("token").toString());
		}
		ObjectMapper objectMapper = new ObjectMapper();
		String json = null;
		try {
			json = objectMapper.writeValueAsString(inputInfo);
			System.out.println(json);
		} catch (JsonProcessingException e) {
			throw new BusinessException(e);
		}
		checkoutInfo.setInputInfo(json);

		Repository defaultRepo = repositoryService.findDefaultRepository();
        String uuid = null;
		try {
			uuid = crossStorageApi.createOrUpdate(defaultRepo, checkoutInfo);
			Log.info("checkoutInfo instance {} created", uuid);
		} catch (Exception ex) {
			throw new BusinessException(ex);
		}

		// retrieve apiKey from credential
		//MvCredential credential =
		//credentialHelperService.getCredential("stripe.com");
		//if(credential==null){
		//	Log.error("stripe.com credential not found");
		//	throw new BusinessException("technical error");
		//}
        //Log.info("========================================================================================");
        //Log.info("credential.getApiKey()="+credential.getApiKey());
		
      //Stripe.apiKey = "sk_test_51ME7KzF8O6FLWQWJwzBsPG7XXyr1uVSjsRF7J1OkLvusWPUi3aehz6xntJHirHqVdjsdadTHbRF5w9atu3b9QhPk002fWXABem";
      Stripe.apiKey = "sk_test_51MTznEJQmmmLLXjqamKcb0YpB09K432YXD4lSumZIi2vXOaDqW0pditpdN7ifHHAhxNj2a647vWcwYA5rhrNG8Na00BsAHuNF3";

		try {
			SessionCreateParams params = SessionCreateParams.builder()
					.setMode(SessionCreateParams.Mode.PAYMENT)
					.setSuccessUrl("https://unikbase-infra.github.io/env-onboarding-dev/#/success")
					.setCancelUrl("https://unikbase-infra.github.io/env-onboarding-dev/#/cancel")
					.setAutomaticTax(
							SessionCreateParams.AutomaticTax.builder()
									.setEnabled(true)
									.build())
					.addLineItem(
							SessionCreateParams.LineItem.builder()
									.setQuantity(1L)
									//.setPrice("price_1MLsFkF8O6FLWQWJ8uBLWETX")
              						//.setPrice("price_1MTjrKF8O6FLWQWJC7n4Qkfj")
              						//.setPrice("price_1MTjsRF8O6FLWQWJQ0jfbhL0")
                                    //.setPrice("price_1MU8GEJQmmmLLXjqVgkTq3NC")
              					    //.setPrice("price_1MU8mFJQmmmLLXjqtkieBGY6")
              							.setPrice("price_1MU8ueJQmmmLLXjqGx5Qjblb")
									.build())
                    .putMetadata("checkoutInfoId",uuid) 
                    .putMetadata("customerEmail",checkoutInfo.getEmail())
					.build();
			Session session = Session.create(params);
          
			Log.info("session {}", session);
			responseUrl = session.getUrl();
			Log.info("responseUrl {}", responseUrl);
		} catch (StripeException ex) {
			Log.error("Stripe error", ex);
		}
	}

}