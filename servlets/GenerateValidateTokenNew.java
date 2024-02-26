package com.basfeupf.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import com.basfeupf.core.services.*;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.basfeupf.core.constants.Basf_Constant;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Component(service = Servlet.class)
@SlingServletResourceTypes(resourceTypes = "/bin/eupf/token/generatevalidate", extensions = "json", methods = {
		HttpConstants.METHOD_GET })
public class GenerateValidateTokenNew extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;
	
	@Reference
	HttpCallerService httpCallerService;

	@Reference
	AzureAuthServiceNew azureAuthService;

	@Reference
	AuthConfigService authConfigService;

	@Reference
	EupfServiceNew eupfService;

	@Reference
	JobManager jobManager;

	@Reference
	TalendServise talendServise;

	@Reference
	AppAccessTokenService appAccessTokenService;

	@Reference
	UserDetailsService userDetailsService;


	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		JsonObject responseJson = handlePost(request, response);
		out.println(responseJson);
	}

	private JsonObject handlePost(SlingHttpServletRequest request, SlingHttpServletResponse response) {

		JsonObject responseJson = new JsonObject();
		boolean isValid = false;
		String talendxApiKey="";
		String talendxApiClientId="";
		String talendurl="";
		String state="";
		try {
			if(request.getParameter("state")!=null ){
				if(!request.getParameter("state").equals("")){
					state = request.getParameter("state");
				}

			}

			String reqid_token = request.getParameter("id_token");
			if (Objects.nonNull(state)) {

				talendxApiKey=authConfigService.getEupf_apigee_talend_key();
				talendxApiClientId = authConfigService.getEupf_apigee_talend_client_id();
				talendurl=authConfigService.getTalendEndpointUrl()+Basf_Constant.TALEND_LOGIN_REG_ENDPOINT;



			}

			String nonce = authConfigService.getNonce();
			// call azure API to get JWT token from access code for login register
			JsonObject tokenJson=new JsonObject();
			if(Objects.nonNull(reqid_token) && !reqid_token.equalsIgnoreCase("")) {
				tokenJson.addProperty("id_token", reqid_token);
				JsonObject statePayLoadJson = azureAuthService.getPayloadJson(reqid_token);
				state = statePayLoadJson.get("userJourney").getAsString();
			} else {
				tokenJson = eupfService.getAzureToken(request, response);

			}

			String[] azureErrorcode= {"AADB2C90002","AADB2C90006","AADB2C90007","AADB2C90008","AADB2C90010","AADB2C90011","AADB2C90012","AADB2C90013","AADB2C90014","AADB2C90016","AADB2C90017","AADB2C90018","AADB2C90019","AADB2C90021","AADB2C90022","AADB2C90023","AADB2C90025","AADB2C90027","AADB2C90028","AADB2C90031","AADB2C90035","AADB2C90036","AADB2C90037","AADB2C90039","AADB2C90040","AADB2C90043","AADB2C90044","AADB2C90046","AADB2C90047","AADB2C90048"};

			if (Objects.nonNull(state) && state.equals(Basf_Constant.ABORT)) {
				responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.ABORT);
				responseJson.addProperty(Basf_Constant.ERROR_MSG, Basf_Constant.ABORT);
				return responseJson;
			}

			if (tokenJson.has("error_description")) {

				responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
				responseJson.addProperty(Basf_Constant.ERROR_MSG, tokenJson.get("error_description").getAsString());
				return responseJson;
			}



			//state as abort

			// check if aud parameter matches

			String client_id = authConfigService.getAzure_clientId();

			tokenJson.addProperty("nonce", nonce);

			if (Objects.nonNull(state)) {
				tokenJson.addProperty("state", state);
			}

			// Verify JWT Token add whether it is same origin or different origin
			JsonObject isValidToken = azureAuthService.isValidToken(tokenJson);

			if (isValidToken.get("isvalid").getAsBoolean()) {
				isValid = true;
				setCookieInResponse(tokenJson, response);

			} else {
				isValid = false;
			}
			responseJson.add("login", tokenJson);
			String id_token = tokenJson.get("id_token").getAsString();

			String request_origin = isValidToken.get("request_origin").getAsString();

			if (isValid) {


				Cookie[] cookies = request.getCookies();
//				if (Objects.nonNull(state) && state.equals(Basf_Constant.FORGOT_PASSWORD)) {
//					deleteCookie(cookies, state, response);
//					responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_SUCCESS);
//					return responseJson;
//				}

				JsonObject payloadJson = azureAuthService.getPayloadJson(id_token);
				payloadJson.addProperty("jwt_token", id_token);
				String email = payloadJson.get("email").getAsString();
				String[] emaildomain = authConfigService.get_email().split(",");
				boolean same_domain = false;
				for (int i = 0; i < emaildomain.length; i++) {
					if (StringUtils.isNotBlank(email) && email.toLowerCase().contains(emaildomain[i])) {
						same_domain = true;
						break;
					}
				}
				responseJson.addProperty("same_domain", same_domain);
				if(request_origin.equals("same") && same_domain){
					responseJson.addProperty("redirect", true);
					return responseJson;
				}
				//origin different but same domain
				boolean skip_user_insert_flow = false;
				if (request_origin.equals("different") && same_domain) {
					skip_user_insert_flow = true;
				}


				String linkedstatus = "";
				String contactid = "";
				String accountid = "";

				if (!skip_user_insert_flow) {


					/********************Get User details from AWS*********************************/
					JsonObject userRequestJson = new JsonObject();
					String sub = payloadJson.has("sub") ? payloadJson.get("sub").getAsString() : "";
					userRequestJson.addProperty("sub", sub);
					userRequestJson.addProperty("type", "get_user_id_sub");
					JsonObject userIdJson = eupfService.callAwsRestService(userRequestJson);
					String dbSegment = "";
					String dbUUID = "";

					String selected_account_type = "";
					JsonObject userJson = new JsonObject();
					if (userIdJson.has(Basf_Constant.STATUS) && userIdJson.get(Basf_Constant.STATUS).getAsString().equals(Basf_Constant.STATUS_SUCCESS)) {
						if (userIdJson.has(Basf_Constant.DATA)) {
							JsonArray userIdArray = userIdJson.get(Basf_Constant.DATA).getAsJsonArray();
							for (JsonElement userElement : userIdArray) {
								userJson = userElement.getAsJsonObject();


								if (userJson.has("requested_business_segment")) {
									dbSegment = userJson.get("requested_business_segment").getAsString();
								}

								if (userJson.has("adobe_uuid")) {
									dbUUID = userJson.get("adobe_uuid").getAsString();
									payloadJson.addProperty("adobe_uuid", dbUUID);
								}

								if (userJson.has("contactId")) {
									contactid = userJson.get("contactId").getAsString();
								}

								if (userJson.has("account_id")) {
									accountid = userJson.get("account_id").getAsString();
								}

								if (userJson.has("selected_account_type")) {
									selected_account_type = userJson.get("selected_account_type").getAsString();
								}
							}
						}
					}

					JsonObject mdmRequestObject = getRequest(state, userJson, payloadJson);
					/*****************************************************/

					responseJson.add("userinfo", payloadJson);
					responseJson.addProperty("selected_account_type", selected_account_type);


					//insert or update data in db
					JsonObject userDetilsjson = new JsonObject();
                    if (payloadJson.has("businessType")) {
						payloadJson.addProperty("extension_user_type", talendServise.getMdmValue(authConfigService.getAccounttype(), getValue(payloadJson, "businessType")));
					}
					userDetilsjson = eupfService.insertIntoUserDetils(payloadJson);

					if (Objects.nonNull(userDetilsjson)) {
						if (userDetilsjson.has(Basf_Constant.ERROR_MSG)) {

							setCookieInResponse(tokenJson, response);
							responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_SUCCESS);

							if (userDetilsjson.get(Basf_Constant.ERROR_MSG).getAsString()
									.equals(Basf_Constant.STATUS_SUCCESS)) {
								// signup email
								if (state.equalsIgnoreCase(Basf_Constant.REGISTER)
										|| state.equalsIgnoreCase(Basf_Constant.PARTNER)
										|| state.equalsIgnoreCase(Basf_Constant.LITE_REGISTER)) {
									if (!authConfigService.getDisable_mail()) {
										sendEmail(payloadJson);

									}
								}
							}
						}
					}

					if (state.equalsIgnoreCase(Basf_Constant.FORGOT_PASSWORD)) {
						if (!authConfigService.getDisable_mail()) {
							sendEmailForgot(payloadJson);

						}

					}

					// talend

					JsonObject talendJson = talendServise.talendAPI(id_token, state, talendxApiKey, talendxApiClientId, talendurl, userDetilsjson);
					if(state.equalsIgnoreCase(Basf_Constant.REGISTER) || state.equalsIgnoreCase(Basf_Constant.PARTNER)){

					}
					String lead_id = "";
					if(talendJson.has("talendjson")){
						lead_id = getValue(talendJson.get("talendjson").getAsJsonObject().get("Profile_Data").getAsJsonObject(), "Lead_Id");
					}else if(state.equals(Basf_Constant.PARTNER)) {
						lead_id =  getValue(talendJson.get("Profile_Data").getAsJsonObject(), "Lead_ID");
					} else {
						lead_id =  getValue(talendJson.get("Profile_Data").getAsJsonObject(), "Lead_ID");
					}


					if (userDetilsjson.has("type")
							&& userDetilsjson.get("type").getAsString().equalsIgnoreCase("insert")
							&& StringUtils.isNotBlank(lead_id)) {
						payloadJson.addProperty("lead_id", lead_id);
						eupfService.updateLeadId(payloadJson);

						JsonObject azureRequestObject = new JsonObject();
						azureRequestObject.addProperty("grant_type", "client_credentials");
						azureRequestObject.addProperty("scope", "https://graph.microsoft.com/.default");
						String azureToken = appAccessTokenService.getAppAccessToken(azureRequestObject)
								.get("access_token").getAsString();
						JsonObject updateB2CLeadId = new JsonObject();
						updateB2CLeadId.addProperty(
								"extension_" + authConfigService.getAzure_extension_no() + "_prospect_id", lead_id);
						userDetailsService.updateUser(sub, updateB2CLeadId, azureToken);
					}

					//JsonObject talendJson = httpCallerService.callGet("https://run.mocky.io/v3/6d8e4a6b-1151-4e4b-aa09-85876c1eb668");

//					//insert or update data in db
//					JsonObject userDetilsjson = new JsonObject();
//					payloadJson.addProperty("lead_id", talendJson.get("talendjson").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Lead_Id").getAsString());
//					userDetilsjson = eupfService.insertIntoUserDetils(payloadJson);
//
//					if (Objects.nonNull(userDetilsjson)) {
//						if (userDetilsjson.has(Basf_Constant.ERROR_MSG)) {
//
//							setCookieInResponse(tokenJson, response);
//							responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_SUCCESS);
//
//							if (userDetilsjson.get(Basf_Constant.ERROR_MSG).getAsString()
//									.equals(Basf_Constant.STATUS_SUCCESS)) {
//								// signup email
//								if (state.equalsIgnoreCase(Basf_Constant.REGISTER) || state.equalsIgnoreCase(Basf_Constant.PARTNER)) {
//									if (!authConfigService.getDisable_mail()) {
//										sendEmail(payloadJson);
//
//									}
//								}
//							}
//						}
//					}

					// check if talend api response is acknowledege then directly jump db inserrt

					if (Objects.isNull(talendJson)) {
						responseJson.addProperty("talendstatus", "error");
						return responseJson;
					}
					try {
						//JsonObject partnerTalendJson = talendServise.talendAPI(id_token, Basf_Constant.LOGIN, talendxApiKey, talendurl, userDetilsjson);
//						JsonObject headerJson = new JsonObject();
//						headerJson.addProperty("x-api-key", talendxApiKey);
//						headerJson.addProperty("x-id-token", id_token);
//						JsonObject partnerJsonRequest = new JsonObject();
//						partnerJsonRequest.addProperty("Request_Type", "Login");
//						partnerJsonRequest.addProperty("Request_Attribute", "Profile");
//						partnerJsonRequest.addProperty("EUPF_ID", sub);
//						partnerJsonRequest.addProperty("Email_Address", email);
//						partnerJsonRequest.addProperty("Contact_Id", payloadJson.get("extension_contact_id").getAsString());
//						partnerJsonRequest.addProperty("account_id", payloadJson.get("extension_account_number").getAsString());
//						partnerJsonRequest.addProperty("Lead_ID", lead_id);
//
//						JsonObject partnerTalendJson = talendServise.callPost(partnerJsonRequest, talendurl, headerJson);
						linkedstatus = talendJson.get("Profile_Data").getAsJsonObject().get("Status").getAsString();
						responseJson.add("talendjson", talendJson);
						JsonArray accounttype = talendJson.get("Profile_Data").getAsJsonObject().get("Account_Types").getAsJsonArray();
						boolean mismatchlogin = false;
						String basesegment = payloadJson.has("extension_user_Registration_bs") ? payloadJson.get("extension_user_Registration_bs").getAsString() : "";
//						if (!basesegment.equalsIgnoreCase("")) {
							for (int i = 0; i < accounttype.size(); i++) {
								JsonArray segment = accounttype.get(i).getAsJsonObject().get("Segment").getAsJsonArray();
								for (int j = 0; j < segment.size(); j++) {
									if (segment.get(j).getAsJsonObject().get("bus_segment_name").getAsString().equalsIgnoreCase(basesegment)) {
										mismatchlogin = true;
									}
								}

							}

							responseJson.addProperty("mismatchlogin", mismatchlogin);
							responseJson.addProperty("mismatchloginsegment", basesegment);

							if(state.equals(Basf_Constant.PARTNER) && linkedstatus.equals(Basf_Constant.LINKED)){
								JsonObject partnerJson = talendJson.get("Profile_Data").getAsJsonObject();
								Gson gson = new Gson();
								JsonObject azureRequestObject = new JsonObject();
								azureRequestObject.addProperty("grant_type", "client_credentials");
								azureRequestObject.addProperty("client_id", authConfigService.getAzure_clientId());
								azureRequestObject.addProperty("client_secret", "c_.83tsRoA.-CC6m33FldL5UBI9zT9GNwP");
								azureRequestObject.addProperty("scope", "https://graph.microsoft.com/.default");


								//azureRequestObject.add("userObject", userObject);
//								JsonObject azureTalendObj = new JsonObject();
//								azureTalendObj.addProperty("EUPF_ID", partnerTalendJson.get("Profile_Data").getAsJsonObject().get("EUPF_ID").getAsString());
//								azureTalendObj.addProperty("Request_Attribute", "Profile");
//								azureTalendObj.addProperty("Request_Type", "Update");
//								azureTalendObj.addProperty("Contact_Id", partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Contact_Id").getAsString());
//								azureTalendObj.addProperty("Account_BASF_ID", partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Account_BASF_ID").getAsString());
//								azureTalendObj.addProperty("Account_Type", selected_account_type);
//								azureTalendObj.addProperty("Business_Segment_Id", "");
//
//								JsonArray attributes = new JsonArray();
//								String attributeObjString = "[{\"attr_key\":\"Contact_First_Name\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Contact_First_Name").getAsString()+"\",\"attr_type\":\"CORE\"},{\"attr_key\":\"Contact_Last_Name\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Contact_Last_Name").getAsString()+"\",\"attr_type\":\"CORE\"},{\"attr_key\":\"Email_Address\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Email_Address").getAsString()+"\",\"attr_type\":\"CONTACT_COMMUNICATION\"},{\"attr_key\":\"Phone_Number\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Phone_Number").getAsString()+"\",\"attr_type\":\"CONTACT_COMMUNICATION\"},{\"attr_key\":\"Business_Name\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Business_Name").getAsString()+"\",\"attr_type\":\"CORE\"},{\"attr_key\":\"Mailing_Address\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Mailing_Address").getAsString()+"\",\"attr_type\":\"CONTACT_ADDRESS\"},{\"attr_key\":\"City\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("City").getAsString()+"\",\"attr_type\":\"CONTACT_ADDRESS\"},{\"attr_key\":\"State\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("State").getAsString()+"\",\"attr_type\":\"CONTACT_ADDRESS\"},{\"attr_key\":\"Zipcode\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Zipcode").getAsString()+"\",\"attr_type\":\"CONTACT_ADDRESS\"},{\"attr_key\":\"Country\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Country").getAsString()+"\",\"attr_type\":\"CONTACT_ADDRESS\"},{\"attr_key\":\"Mobile_Phone\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Mobile_Phone").getAsString()+"\",\"attr_type\":\"CONTACT_COMMUNICATION\"},{\"attr_key\":\"Preferred_Language\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Preferred_Language").getAsString()+"\",\"attr_type\":\"CORE\"},{\"attr_key\":\"Email_Opt_In\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Email_Opt_In").getAsString()+"\",\"attr_type\":\"CONTACT_COMMUNICATION\"},{\"attr_key\":\"Text_Opt_In\",\"attr_value\":\""+partnerTalendJson.get("Profile_Data").getAsJsonObject().get("Text_Opt_In").getAsString()+"\",\"attr_type\":\"CONTACT_COMMUNICATION\"},{\"attr_key\":\"\",\"attr_value\":\"No\",\"attr_type\":\"\"}]";
//
//								JsonArray attributeArray = gson.fromJson(attributeObjString, JsonArray.class);
//								azureTalendObj.add("Attributes", attributeArray);
//								azureTalendObj.addProperty("Email_Address", "");
//								azureTalendObj.addProperty("type", "talend-user-data");
//
//								azureRequestObject.add("talendUserObject", azureTalendObj);
//								azureRequestObject.addProperty("sub", payloadJson.get("sub").getAsString());

								JsonObject userObject = new JsonObject();
								userObject.addProperty("displayName", getValue(partnerJson,"Contact_First_Name") + " " + getValue(partnerJson,"Contact_Last_Name")); //talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Contact_First_Name").getAsString()+" "+talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Contact_Last_Name").getAsString());
								userObject.addProperty("givenName", getValue(partnerJson,"Contact_First_Name")); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Contact_First_Name").getAsString());
								userObject.addProperty("surname",getValue(partnerJson,"Contact_Last_Name")); //talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Contact_Last_Name").getAsString());
								userObject.addProperty("country",getValue(partnerJson,"Country"));// talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Country").getAsString());
								userObject.addProperty("preferredLanguage",getValue(partnerJson,"Preferred_Language")); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Preferred_Language").getAsString());
								String azureExtention = "extension_"+authConfigService.getAzure_extension_no()+"_";
								userObject.addProperty(azureExtention+"mailing_address",getValue(partnerJson,"Mailing_Address")); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Mailing_Address").getAsString());
								userObject.addProperty(azureExtention+"mailing_city",getValue(partnerJson,"City")); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("City").getAsString());
								userObject.addProperty(azureExtention+"mailing_state",getValue(partnerJson,"State")); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("State").getAsString());
								userObject.addProperty(azureExtention+"mailing_postalCode", getValue(partnerJson,"Zipcode")); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Zipcode").getAsString());
								userObject.addProperty(azureExtention+"email_communication_consent", getValue(partnerJson,"Email_Opt_In")); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Email_Opt_In").getAsBoolean());
								userObject.addProperty(azureExtention+"MobileConsent", getValue(partnerJson,"Text_Opt_In")); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Text_Opt_In").getAsBoolean());
								userObject.addProperty(azureExtention+"business_name", getValue(partnerJson,"Business_Name")); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Business_Name").getAsString());
								userObject.addProperty(azureExtention+"account_number", partnerJson.get("Accounts").getAsJsonArray().get(0).getAsJsonObject().get("account_id").getAsString()); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Account_BASF_ID").getAsString());
								userObject.addProperty(azureExtention+"contact_id", getValue(partnerJson,"Contact_Id")); // talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Contact_Id").getAsString());
								//userObject.addProperty("lead_id", lead_id);
								JsonArray businessPhones = new JsonArray();
								businessPhones.add(getValue(partnerJson,"Mobile_Phone"));//talendJson.get("Profile_Data").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Mobile_Phone").getAsString());
								userObject.add("businessPhones", businessPhones);

								JsonObject accessToken = appAccessTokenService.getAppAccessToken(azureRequestObject);
								String userID = payloadJson.get("sub").getAsString();
								JsonObject jsonObject = userDetailsService.updateUser(userID, userObject, accessToken.get("access_token").getAsString());
							}
//						}
					} catch (Exception e) {
//						linkedstatus = talendJson.get("root").getAsJsonObject().get("ContactData").getAsJsonObject().get("Status").getAsString();
//						JsonObject talendJsonModified = new JsonObject();
//						talendJsonModified.add("Profile_Data", talendJson.get("root").getAsJsonObject().get("ContactData"));
//						responseJson.add("talendjson", talendJsonModified);
						JsonObject talendJsonModified = new JsonObject();
						if(talendJson.has("talendjson")){
							linkedstatus = talendJson.get("talendjson").getAsJsonObject().get("Profile_Data").getAsJsonObject().get("Status").getAsString();
							talendJsonModified.add("Profile_Data", talendJson.get("talendjson").getAsJsonObject().get("Profile_Data"));
						}else {
							linkedstatus = talendJson.get("Profile_Data").getAsJsonObject().get("Status").getAsString();
							talendJsonModified.add("Profile_Data", talendJson.get("Profile_Data").getAsJsonObject());
						}
						responseJson.add("talendjson", talendJsonModified);
					}
					responseJson.addProperty("talendstatus", linkedstatus);


					if (linkedstatus.equalsIgnoreCase("Acknowledged")) {
						// get segment name and id from aws
						JsonObject requestJsonSegment = new JsonObject();
						String segment_name = "";
						if (payloadJson.has("extension_user_Registration_bs")) {
							if (!payloadJson.get("extension_user_Registration_bs").getAsString().equalsIgnoreCase("")
									&& payloadJson.get("extension_user_Registration_bs").getAsString() != null) {
								segment_name = payloadJson.get("extension_user_Registration_bs").getAsString();
							} else {
								if (!dbSegment.equalsIgnoreCase("")) {
									segment_name = dbSegment;
								} else {
									segment_name = "Turf & Ornamental";
								}
							}
						} else {
							if (!dbSegment.equalsIgnoreCase("")) {
								segment_name = dbSegment;
							} else {
								segment_name = "Turf & Ornamentals";
							}
						}

						requestJsonSegment.addProperty("segment_id", segment_name);
						requestJsonSegment.addProperty("type", "get_segment_id_from_name");
						JsonObject segmentJson = eupfService.callAwsRestService(requestJsonSegment);
						responseJson.add("segmentdetails", segmentJson);
					} else {
						JsonArray accountTypeJson = new JsonArray();
						if (talendJson.get("Profile_Data").getAsJsonObject().has("Account_Types")) {
							accountTypeJson = talendJson.get("Profile_Data").getAsJsonObject().get("Account_Types").getAsJsonArray();
						}else {
                            accountTypeJson = talendJson.get("Profile_Data").getAsJsonObject().get("Account_Types").getAsJsonArray();
                        }

						responseJson.add("segmentdetails", accountTypeJson);
					}


					responseJson.addProperty("origin", request_origin);
					if ((linkedstatus.equalsIgnoreCase("Not Linked") || linkedstatus.equalsIgnoreCase("Acknowledged")) && request_origin.equals("different")) {
						responseJson.addProperty("origin", "same");
					}

				}

				if (request_origin.equals("same")  && (linkedstatus.equalsIgnoreCase("Not Linked") || linkedstatus.equalsIgnoreCase("Acknowledged") || linkedstatus.equalsIgnoreCase("linked") )) {
					// same origin flow
					//userDetilsjson = eupfService.insertIntoUserDetils(payloadJson);

				} else {
//					different origin flow

					// get app id from client id
					responseJson.addProperty("origin", "different");// remove
					String client_id_payloaad = payloadJson.has("aud") ? payloadJson.get("aud").getAsString() : "";
					String app_id_data = "";
					JsonObject awsRequestJson = new JsonObject();
					awsRequestJson.addProperty("type", "get_app_data_client_id");
					awsRequestJson.addProperty("client_id", client_id_payloaad);
					awsRequestJson.addProperty("country", request.getParameter("country"));
					

					JsonObject responseJsonapp = eupfService.callAwsRestService(awsRequestJson); // add in response
					responseJson.add("appdata", responseJsonapp);
					if (responseJsonapp.has(Basf_Constant.STATUS) && responseJsonapp.get(Basf_Constant.STATUS)
							.getAsString().equals(Basf_Constant.STATUS_SUCCESS)) {
						JsonArray appIdArray = responseJsonapp.get(Basf_Constant.DATA).getAsJsonArray();
						for (JsonElement jsonElement : appIdArray) {
							JsonObject jsonObject = jsonElement.getAsJsonObject();

							if (jsonObject.has("app_id")) {
								app_id_data = jsonObject.get("app_id").getAsString();
							}
						}
					}
					String email1 = payloadJson.get("email").getAsString();
					String [] emaildomain1 = authConfigService.get_email().split(",");
					boolean same_domain1 = false;
					for(int i=0; i<emaildomain1.length; i++){
						if(StringUtils.isNotBlank(email1) && email1.toLowerCase().contains(emaildomain1[i])){
							same_domain1=true;
							break;
						}
					}
					responseJson.addProperty("same_domain", same_domain1);



					// get app specific mandatory attribute

					awsRequestJson = new JsonObject();
					awsRequestJson.addProperty("type", "get_app_data_app_id");
					awsRequestJson.addProperty("app_id", app_id_data);
					awsRequestJson.addProperty("language", request.getParameter("language"));
					JsonObject responseJsonappFormData = eupfService.callAwsRestService(awsRequestJson);

					if((linkedstatus.equalsIgnoreCase("Not Linked") || linkedstatus.equalsIgnoreCase("Acknowledged")) && request_origin.equals("different")) {
						same_domain = true;
						responseJson.addProperty("same_domain", true);

					}
					if(!same_domain) {

						String accountType[] = authConfigService.getAccounttype();
						// get talend api form data
						JsonObject talendJson = responseJson.get("talendjson").getAsJsonObject().get("Profile_Data").getAsJsonObject();
						JsonObject requestJson = new JsonObject();
						requestJson.addProperty("EUPF_ID", getValue(talendJson, "EUPF_ID"));// payloadJson.has("sub") ? payloadJson.get("sub").getAsString() : "");
						requestJson.addProperty("Request_Attribute", "APP");
						requestJson.addProperty("Request_Type", "Fetch");
						requestJson.addProperty("Email_Address", getValue(payloadJson, "email"));
						requestJson.addProperty("language", request.getParameter("language"));
						if (StringUtils.isNotBlank(getValue(talendJson, "Lead_ID"))) {
							requestJson.addProperty("Lead_ID", getValue(talendJson, "Lead_ID"));
						}
						requestJson.addProperty("Contact_Id", getValue(talendJson, "Contact_Id")); //payloadJson.has("extension_contact_id") ? payloadJson.get("extension_contact_id").getAsString() : "");
						requestJson.addProperty("account_id", talendJson.get("Accounts").getAsJsonArray().get(0).getAsJsonObject().get("account_id").getAsString());
//						requestJson.addProperty("Account_BASF_ID", payloadJson.has("extension_account_number")
//								? payloadJson.get("extension_account_number").getAsString()
//								: "");
						requestJson.addProperty("Account_Type", talendServise.getMdmValue(accountType, getValue(payloadJson, "businessType")));
						requestJson.addProperty("Business_Segment_Id", getValue(payloadJson, "extension_user_Registration_bs"));

						if(!contactid.equalsIgnoreCase("")) {
							requestJson.addProperty("Contact_Id",contactid);
						}

						if(!accountid.equalsIgnoreCase("")) {
							requestJson.addProperty("accouint_id", accountid);
						}

						JsonArray talendRequestAttrArray = new JsonArray();
						JsonArray app_data_array = responseJsonappFormData.get(Basf_Constant.DATA).getAsJsonArray();
						for (JsonElement jsonElement : app_data_array) {
							JsonObject jsonObject = jsonElement.getAsJsonObject();
							String attrib_map_id = jsonObject.has("attrib_map_id")
									? jsonObject.get("attrib_map_id").getAsString()
									: "";
							String attr_type = jsonObject.has("crm_attrib_type")
									? jsonObject.get("crm_attrib_type").getAsString()
									: "";

							JsonObject attrJson = new JsonObject();
							attrJson.addProperty("attr_key", attrib_map_id);
							attrJson.addProperty("attr_value", "");
							attrJson.addProperty("attr_type", attr_type);
							talendRequestAttrArray.add(attrJson);
						}

						requestJson.add("Attributes", talendRequestAttrArray);

						String url = authConfigService.getTalendEndpointUrl()+Basf_Constant.TALEND_APP_ENDPOINT;
						//talendServise.talendAPIGeneric(payloadJson.get("jwt_token").getAsString(),  talendxApiKey, talendurl,requestJson );
						//
						JsonObject talendResponseJson = talendServise.talendAPIGeneric(talendxApiClientId,  talendxApiKey, url,requestJson );//httpCallerService.callPost(requestJson, url);
						responseJson.add("talend", talendResponseJson);

						if (Objects.nonNull(talendResponseJson)) {
							// for add attr_value in response
							talendServise.add_attr_value_in_response(talendResponseJson, app_data_array,
									responseJsonappFormData);
							responseJson.add("merged", responseJsonappFormData);
						}
					}else {
						JsonObject allFieldsSubmitted = new JsonObject();
						allFieldsSubmitted.addProperty("allFieldsSubmitted", true);
						responseJson.add("merged", allFieldsSubmitted);
					}

				}

//				login/reg

				// db
//				JsonObject userDetilsjson = eupfService.insertIntoUserDetils(payloadJson);


			} else {
				deleteCookie(request.getCookies(), state, response);
				responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
				if (responseJson.has(Basf_Constant.ERROR_MSG)
						&& responseJson.get(Basf_Constant.ERROR_MSG).getAsString().isEmpty()) {
					responseJson.addProperty(Basf_Constant.ERROR_MSG, "token validation failed");
				}
			}

		} catch (Exception e) {
			responseJson.addProperty(Basf_Constant.STATUS, Basf_Constant.STATUS_FAIL);
			responseJson.addProperty(Basf_Constant.ERROR_MSG, e.getClass().getSimpleName() + " : " + e.getMessage());
			StackTraceElement[] sTElements = e.getStackTrace();
			for (StackTraceElement stackTraceEle : sTElements) {
				String corePackageName = this.getClass().getPackage().getName().split("core")[0] + "core";
				if (stackTraceEle.getClassName().contains(corePackageName)) {
					StringBuffer stringBuffer = new StringBuffer();
					stringBuffer.append("\n{").append("\n\t\"ClassName\" : \"" + stackTraceEle.getClassName() + "\"")
							.append("\n\t\"MethodName\" : \"" + stackTraceEle.getMethodName() + "\",")
							.append("\n\t\"LineNumber\" : \"" + stackTraceEle.getLineNumber() + "\",")
							.append("\n\t\"" + e.getClass().getSimpleName() + "\" : \"" + e.getMessage() + "\"")
							.append("\n}\n");
					logger.error(stringBuffer.toString());
					JsonObject errorJson = new JsonObject();
					errorJson.addProperty("ClassName", stackTraceEle.getClassName());
					errorJson.addProperty("MethodName", stackTraceEle.getMethodName());
					errorJson.addProperty("LineNumber", stackTraceEle.getLineNumber());
					errorJson.addProperty(e.getClass().getSimpleName(), e.getMessage());
					responseJson.add(Basf_Constant.ERROR_JSON, errorJson);
					break;
				}
			}
		}
		return responseJson;
	}

	private JsonObject getRequest(String state, JsonObject userObject, JsonObject payloadJson){

		String sub = getValue(payloadJson, "sub");
		String email = getValue(payloadJson, "email");
		String firstname = getValue(payloadJson, "firstname");
		String lastname = getValue(payloadJson, "lastname");
		String mailing_address = getValue(payloadJson, "extension_mailing_address");
		String mailing_city = getValue(payloadJson, "extension_mailing_city");
		String mailing_state = getValue(payloadJson, "extension_mailing_state");
		String mailing_postalCode = getValue(payloadJson, "extension_mailing_postalCode");
		String country = getValue(payloadJson, "country");
		String telephoneNumber = getValue(payloadJson, "telephoneNumber");
		String business_name = getValue(payloadJson, "extension_business_name");
		String business_segment = getValue(payloadJson, "extension_user_Registration_bs");
		String contact_id = getValue(payloadJson, "extension_contact_id");
		String account_id = getValue(payloadJson, "extension_account_number");
		String user_type = getValue(payloadJson, "businessType");
		logger.debug("UserType: {}",user_type);
		if(contact_id == ""){
			logger.debug("ContactID is not present in jwt");
		}else if(account_id == ""){
			logger.debug("AccountID is not present in jwt");
		}
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("Request_Attribute", "Profile");
		jsonObject.addProperty("EUPF_ID", sub);
		jsonObject.addProperty("Email_Address", email);


		if(state.equalsIgnoreCase(Basf_Constant.REGISTER)){
			jsonObject.addProperty("Request_Type", "Registration");
			jsonObject.addProperty("Contact_First_Name", firstname);
			jsonObject.addProperty("Contact_Last_Name", lastname);
			jsonObject.addProperty("Mailing_Address", mailing_address);
			jsonObject.addProperty("City", mailing_city);
			jsonObject.addProperty("State", mailing_state);
			jsonObject.addProperty("Zipcode", mailing_postalCode);
			jsonObject.addProperty("Country", country);
			jsonObject.addProperty("Phone_Number", telephoneNumber);
			jsonObject.addProperty("Business_Name", business_name);
			jsonObject.addProperty("Business_Segment", business_segment);
			jsonObject.addProperty("Account_Type", user_type);
		}
		if(state.equalsIgnoreCase(Basf_Constant.LOGIN)){
			jsonObject.addProperty("Request_Type", "Login");
		}

		return jsonObject;
	}

	private String getValue(JsonObject jsonObject, String value){
		try {
			return jsonObject.get(value).getAsString();
		}catch (Exception n){
			return "";
		}
	}

	private void sendEmail(JsonObject payloadJson) {

		String firstName = payloadJson.get("firstname").getAsString();
		String lastName = payloadJson.get("lastname").getAsString();
		String email = payloadJson.get("email").getAsString();

		String body = authConfigService.getSignupEmailBody();
		body = body.replace("First Name Last Name", firstName + " " + lastName);
		String subject = authConfigService.getSignupEmailSubject();

		Map<String, Object> jobProperties = new HashMap<>();
		if (Objects.nonNull(email)) {
			jobProperties.put("toEmail", email);
		}
		if (Objects.nonNull(subject)) {
			jobProperties.put("subject", subject);
		}
		if (Objects.nonNull(body)) {
			jobProperties.put("body", body);
		}
		jobManager.addJob(Basf_Constant.JOB_TOPIC_EMAIL, jobProperties);

	}

	private void sendEmailForgot(JsonObject payloadJson) {

		String firstName = payloadJson.get("firstname").getAsString();
		String lastName = payloadJson.get("lastname").getAsString();
		String email = payloadJson.get("email").getAsString();

		String body = authConfigService.getSignup_forgot_password_body();
		body = body.replace("First Name Last Name", firstName + " " + lastName);
		String subject = authConfigService.getSignup_forgot_password_subject();

		Map<String, Object> jobProperties = new HashMap<>();
		if (Objects.nonNull(email)) {
			jobProperties.put("toEmail", email);
		}
		if (Objects.nonNull(subject)) {
			jobProperties.put("subject", subject);
		}
		if (Objects.nonNull(body)) {
			jobProperties.put("body", body);
		}
		jobManager.addJob(Basf_Constant.JOB_TOPIC_EMAIL, jobProperties);

	}

	private void setCookieInResponse(JsonObject tokenJson, SlingHttpServletResponse response) {

//		for (String key : tokenJson.keySet()) {
//			if (!key.equals("scope")) {
//				String value = tokenJson.get(key).toString();
//				Cookie cookie = new Cookie(key, value);
//				cookie.setPath("/");
//				cookie.setMaxAge(3600);
//				// cookie.setHttpOnly(true);
//				// cookie.setSecure(true);
//				if(!authConfigService.getCookie_domain_name().equalsIgnoreCase("")) {
//					cookie.setDomain(authConfigService.getCookie_domain_name());
//				}
//
//				response.addCookie(cookie);
//			}
//		}
		JsonObject jsonObject = new JsonObject();
		try {
			JsonObject payLoadJson = azureAuthService.getPayloadJson(tokenJson.get("id_token").getAsString());
			jsonObject.addProperty("firstName", payLoadJson.get("firstname").getAsString());
			jsonObject.addProperty("lastName", payLoadJson.get("lastname").getAsString());
			String userObj = Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
			Cookie cookie = new Cookie("userCookie", userObj);
			cookie.setPath("/");
			cookie.setMaxAge(3600);
			if(!authConfigService.getCookie_domain_name().equalsIgnoreCase("")) {
				cookie.setDomain(authConfigService.getCookie_domain_name());
			}

			response.addCookie(cookie);

		}catch (Exception e){}

	}

	private void deleteCookie(Cookie[] cookies, String state, SlingHttpServletResponse response) {

		for (Cookie cookie : cookies) {
			String cookieName = cookie.getName();
			if (cookieName.equals("access_token") || cookieName.equals("id_token") || cookieName.equals("token_type")
					|| cookieName.equals("not_before") || cookieName.equals("expires_in")
					|| cookieName.equals("expires_on") || cookieName.equals("resource") || cookieName.equals("resource")
					|| cookieName.equals("id_token_expires_in") || cookieName.equals("profile_info")
					|| cookieName.equals("refresh_token") || cookieName.equals("refresh_token_expires_in")
					|| cookieName.equals("nonce") || cookieName.equals("state")) {

				Cookie ck = new Cookie(cookieName, null);
				ck.setPath("/");
				ck.setMaxAge(0);
				if(!authConfigService.getCookie_domain_name().equalsIgnoreCase("")) {
					ck.setDomain(authConfigService.getCookie_domain_name());
				}
				response.addCookie(ck);
			}
		}
	}

}
