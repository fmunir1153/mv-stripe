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
    public static final String SUCCESS_URL = "https://unikbase-infra.github.io/env-onboarding-dev/#/success";
    public static final String CANCEL_URL = "https://unikbase-infra.github.io/env-onboarding-dev/#/cancel";
    public static Map < String, String > priceMap;

    static {
        priceMap = new HashMap < > ();
        priceMap.put("0", "0");
        priceMap.put("1", "price_1MWOHoJQmmmLLXjqRUnYQ69J");
        priceMap.put("2", "price_1MU8ueJQmmmLLXjqGx5Qjblb");
        priceMap.put("3", "price_1MWOILJQmmmLLXjqLjuLjV9y");
        priceMap.put("4", "price_1MWOHoJQmmmLLXjqRUnYQ69J");
        priceMap.put("5", "price_1MWOIgJQmmmLLXjqT9epLq7d");
    }
    private String responseUrl = "";

    public String getResponseUrl() {
        return responseUrl;
    }

    @Override
    public void execute(Map < String, Object > parameters) throws BusinessException {
        Log.info("received {}", parameters);
        String priceId = "2";
        StrCheckoutInfo checkoutInfo = new StrCheckoutInfo();
        checkoutInfo.setCreationDate(Instant.now());

        // as long as we have an email we process the payment
        /*if (parameters.containsKey("email")) {
		checkoutInfo.setEmail(parameters.get("email").toString());
	    } else {
		endpointResponse.setStatus(400);
		endpointResponse.setErrorMessage("missing email");
	    }*/

        if (parameters.containsKey("price_id")) {
            priceId = parameters.get("price_id").toString();
            if (priceMap.get(priceId) == null) {
                throw new BusinessException("No Price is defined against price_id provided");
            } else if ("0".equals(priceMap.get(priceId))) {
                if (parameters.get("email") == null) {
                    endpointResponse.setStatus(400);
                    endpointResponse.setErrorMessage("missing email, if provided we can send email");
                } else {
                    responseUrl = this.endpointRequest.getRequestURL().toString().substring(0, this.endpointRequest.getRequestURL().toString().indexOf("/rest/")) + "/rest/stripeNoPaymentCheckoutSuccess?customerEmail=" + parameters.get("email").toString();
                }
                return;
            }
        }

        Map < String, String > inputInfo = new HashMap < > ();
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
        //MvCredential credential = credentialHelperService.getCredential("stripe.com");
        //if (credential == null) {
            Log.error("stripe.com credential not found, hardcoding secret api keys");
            //throw new BusinessException("technical error");
            Stripe.apiKey = "sk_test_51MTznEJQmmmLLXjqamKcb0YpB09K432YXD4lSumZIi2vXOaDqW0pditpdN7ifHHAhxNj2a647vWcwYA5rhrNG8Na00BsAHuNF3";
        /*} else {
            Log.info("========================================================================================");
            Log.info("credential.getApiKey()=" + credential.getApiKey());
            Log.info("Api key should be     =sk_test_51MTznEJQmmmLLXjqamKcb0YpB09K432YXD4lSumZIi2vXOaDqW0pditpdN7ifHHAhxNj2a647vWcwYA5rhrNG8Na00BsAHuNF3");
            Stripe.apiKey = credential.getApiKey();
        }*/


        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(SUCCESS_URL)
                .setCancelUrl(CANCEL_URL)
                .setAutomaticTax(
                    SessionCreateParams.AutomaticTax.builder()
                    .setEnabled(true)
                    .build())
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                    .setQuantity(1 L)
                    .setPrice(priceMap.get(priceId))
                    .build())
                .putMetadata("checkoutInfoId", uuid)
                //.putMetadata("customerEmail",checkoutInfo.getEmail())
                .putMetadata("price", priceMap.get(priceId))
                .putMetadata("tpkId", inputInfo.get("tpk_id"))
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
