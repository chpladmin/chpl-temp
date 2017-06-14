package gov.healthit.chpl.tempApp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.healthit.chpl.dao.EntityCreationException;
import gov.healthit.chpl.dao.EntityRetrievalException;
import gov.healthit.chpl.domain.CertificationBody;
import gov.healthit.chpl.domain.CertifiedProductSearchDetails;
import gov.healthit.chpl.domain.KeyValueModel;
import gov.healthit.chpl.domain.ListingUpdateRequest;
import gov.healthit.chpl.domain.concept.CertificationEditionConcept;
import gov.healthit.chpl.domain.search.BasicSearchResponse;
import gov.healthit.chpl.domain.search.CertifiedProductFlatSearchResult;
import gov.healthit.chpl.entity.CertificationStatusType;
import gov.healthit.chpl.tempApp.App;
import gov.healthit.chpl.tempApp.HttpUtil;
import gov.healthit.chpl.tempApp.Token;
import gov.healthit.chpl.web.controller.results.CertificationBodyResults;

@Component("updateCertificationStatusApp")
public class UpdateCertificationStatusApp extends App {
	private static final String CERTIFICATION_NAME = "CCHIT"; 
	
	private static final Logger logger = LogManager.getLogger(UpdateCertificationStatusApp.class);
	
	public static void main(String[] args) throws Exception {
		// setup application
		UpdateCertificationStatusApp updateCertStatus = new UpdateCertificationStatusApp();
		Properties props = updateCertStatus.getProperties();
		
		// Get authentication token for REST call to API
		Token token = new Token(props);
		token.setToken(token.getNewToken(props).getToken());
		// Get Certification Body for CCHIT
		CertificationBody cb = updateCertStatus.getCertificationBody(CERTIFICATION_NAME, token, props);
		// Get certification edition for year 2014
		KeyValueModel certificationEdition = updateCertStatus.getCertificationEdition(CertificationEditionConcept.CERTIFICATION_EDITION_2014.getYear(), props);
		// Get Map<String, Object> certificationStatus for "Withdrawn by Developer"
		Map<String, Object> updatedCertificationStatus = updateCertStatus.getCertificationStatus(CertificationStatusType.WithdrawnByDeveloper, props);
		// Get all listings certified by CCHIT with 2014 edition and 'Retired' (216 total according to spreadsheet/DB)
		List<CertifiedProductSearchDetails> listings = updateCertStatus.getListings(cb.getName(), certificationEdition, CertificationStatusType.Retired, props);
		// Get Map<CertifiedProductDTO, ListingUpdateRequest> for update
		Map<CertifiedProductSearchDetails, ListingUpdateRequest> listingUpdatesMap = updateCertStatus.getListingUpdateRequests(listings, updatedCertificationStatus, props, token);
		// Update each listing's certification status to 'Withdrawn by Developer'
		updateCertStatus.updateListingsCertificationStatus(cb.getId(), listingUpdatesMap, props, token);
	}
	
	private List<CertifiedProductSearchDetails> getListings(String certificationBodyName, KeyValueModel certificationEdition, CertificationStatusType certificationStatusType, Properties props) throws EntityRetrievalException, JsonParseException, JsonMappingException, IOException{
		logger.info("Get listings for " + certificationBodyName + " " + certificationEdition + " " + certificationStatusType);
		String url = props.getProperty("chplUrlBegin") + props.getProperty("basePath") + props.getProperty("getAllCertifiedProductsCertStatus");
		String jsonResponse = HttpUtil.getRequest(url, null, props);
		ObjectMapper mapper = new ObjectMapper();
		BasicSearchResponse results = mapper.readValue(jsonResponse, BasicSearchResponse.class);
		
		List<CertifiedProductSearchDetails> cps = new ArrayList<CertifiedProductSearchDetails>();
		for(CertifiedProductFlatSearchResult cp : results.getResults()){
			if(cp.getAcb().equalsIgnoreCase(certificationBodyName)
					&& cp.getEdition().equalsIgnoreCase(certificationEdition.getName())
					&& cp.getCertificationStatus().equalsIgnoreCase(certificationStatusType.getName())){
				logger.info("Get Certified Product Details for " + cp.getId());
				String cpDetailsUrl = props.getProperty("chplUrlBegin") + props.getProperty("basePath") + String.format(props.getProperty("getCertifiedProductDetails"), cp.getId());
				String cpDetailsJsonResponse = HttpUtil.getRequest(cpDetailsUrl, null, props);
				CertifiedProductSearchDetails cpDetailsResult = mapper.readValue(cpDetailsJsonResponse, CertifiedProductSearchDetails.class);
				cps.add(cpDetailsResult);
			}
		}
		logger.info("Found " + cps.size() + " listings for " + certificationBodyName + " with edition " + certificationEdition.getName() + " and status " + certificationStatusType.getName());
		return cps;
	}

	private void updateListingsCertificationStatus(Long acbId, Map<CertifiedProductSearchDetails, ListingUpdateRequest> cpUpdateMap, Properties props, Token token) throws JsonProcessingException, EntityRetrievalException, EntityCreationException{
		for(CertifiedProductSearchDetails cpDTO : cpUpdateMap.keySet()){
			logger.info("Update listing Certification Status for " + cpDTO.getChplProductNumber());
			Long startTime = System.currentTimeMillis();
			String url = props.getProperty("chplUrlBegin") + props.getProperty("basePath") + props.getProperty("updateCertifiedProduct");
			ObjectMapper mapper = new ObjectMapper();
			ListingUpdateRequest updateRequest = cpUpdateMap.get(cpDTO);
			String json = mapper.writeValueAsString(updateRequest);
			logger.info("JSON Request: " + json);
			HttpUtil.postAuthenticatedBodyRequest(url, null, props, token.getToken(), json);
			token = token.getRefreshedToken(token, props);
			Long elapsedTime = System.currentTimeMillis() - startTime;
			logger.info("Finished updating listing Certification Status for " + cpDTO.getChplProductNumber() + " in " + 
			String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(elapsedTime), 
					TimeUnit.MILLISECONDS.toSeconds(elapsedTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime))));
		}
	}
	
	private CertificationBody getCertificationBody(String certificationBodyName, Token token, Properties props) throws JsonParseException, JsonMappingException, IOException{
		logger.info("Get certification body");
		CertificationBodyResults results = new CertificationBodyResults();
		String url = props.getProperty("chplUrlBegin") + props.getProperty("basePath") + props.getProperty("getAllAcbs");
		String jsonResponse = HttpUtil.getAuthenticatedRequest(url, null, props, token.getToken());
		ObjectMapper mapper = new ObjectMapper();
		results = mapper.readValue(jsonResponse, CertificationBodyResults.class);
		
		for(CertificationBody cb : results.getAcbs()){
			if(cb.getName().equalsIgnoreCase(certificationBodyName)){
				logger.info("Finished getting certification body for " + certificationBodyName);
				return cb;
			}
		}
		return null;
	}
	
	private KeyValueModel getCertificationEdition(String certificationEditionYear, Properties props) throws JsonParseException, JsonMappingException, IOException{
		logger.info("Get certification edition for " + certificationEditionYear);
		String url = props.getProperty("chplUrlBegin") + props.getProperty("basePath") + props.getProperty("getAllCertificationEditions");
		String jsonResponse = HttpUtil.getRequest(url, null, props);
		ObjectMapper mapper = new ObjectMapper();
		Set<KeyValueModel> editionNames = mapper.readValue(jsonResponse, new TypeReference<Set<KeyValueModel>>(){}); 
		
		for(KeyValueModel editionName : editionNames){
			if(editionName.getName().equalsIgnoreCase(certificationEditionYear)){
				logger.info("Finished getting certification edition for " + certificationEditionYear);
				return editionName;
			}
		}
		return null;
	}
	
	private Map<String, Object> getCertificationStatus(CertificationStatusType certificationStatusType, Properties props) throws JsonParseException, JsonMappingException, IOException{
		logger.info("Getting certification status for " + certificationStatusType.getName());
		String url = props.getProperty("chplUrlBegin") + props.getProperty("basePath") + props.getProperty("getAllCertificationStatuses");
		String jsonResponse = HttpUtil.getRequest(url, null, props);
		ObjectMapper mapper = new ObjectMapper();
		Set<KeyValueModel> certStatuses = mapper.readValue(jsonResponse, new TypeReference<Set<KeyValueModel>>(){});
		
		Map<String, Object> certStatusMap = new HashMap<String, Object>();
		for(KeyValueModel certStatus : certStatuses){
			if(certStatus.getName().equalsIgnoreCase(certificationStatusType.getName())){
				certStatusMap.put("date", certStatus.getDescription());
				certStatusMap.put("name", certStatus.getName());
				certStatusMap.put("id", certStatus.getId().toString());
				logger.info("Finished getting certification status for " + certificationStatusType.getName());
				return certStatusMap;
			}
		}
		return certStatusMap;
	}
	
	private Map<CertifiedProductSearchDetails, ListingUpdateRequest> getListingUpdateRequests(List<CertifiedProductSearchDetails> cpDetails, Map<String, Object> newCertificationStatus, Properties props, Token token) throws JsonParseException, JsonMappingException, IOException{
		logger.info("Getting listing update requests");
		Map<CertifiedProductSearchDetails, ListingUpdateRequest> listingUpdatesMap = new HashMap<CertifiedProductSearchDetails, ListingUpdateRequest>();
		for(CertifiedProductSearchDetails cpDetail : cpDetails){
			ListingUpdateRequest listingUpdate = new ListingUpdateRequest();
			listingUpdate.setBanDeveloper(false);
			listingUpdate.setListing(cpDetail);
			listingUpdate.getListing().setCertificationStatus(newCertificationStatus);
			listingUpdatesMap.put(cpDetail, listingUpdate);
		}
		logger.info("Finished getting listing update requests");
		return listingUpdatesMap;
	}

}
