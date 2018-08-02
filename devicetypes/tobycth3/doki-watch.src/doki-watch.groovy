/**
 *  Doki Watch
 *
 *  Copyright 2018 Toby Harris
 *
 *  Author: toby@cth3.com
 *  Date: 7/6/2018
 *
 *  Integrates SmartThings with Doki Watch Messages
 */

preferences {
	input(name: "username", type: "text", title: "Username", required: "true", description: "Doki Username")
	input(name: "password", type: "password", title: "Password", required: "true", description: "Doki Password")
	input(name: "secret", type: "password", title: "Client Secret", required: "true", description: "Doki Client Secret")
	input(name: "region", type: "enum", title: "Doki App Version", required: "true", description: "App Region Version", options: ["US", "Non-US"])
}

metadata {	
	definition (name: "Doki Watch", namespace: "tobycth3", author: "Toby Harris") {
		capability "Polling"
		attribute "message", "string"
		
		valueTile("message", "device.message", width: 6, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat", wordWrap: true) {
			state ("default", label:'${currentValue}')
		}

		main(["message"])
		details(["message"])
	}
}

def installed() {
  init()
}

def updated() {
  unschedule()
  init()
}
  
def init() {
  apiLogout()
  log.info "Setting up Schedule (every 5 minutes)..."
  runEvery1Minute(poll)
}


def poll() {
	//Check Auth first
	checkAuth()

    log.info "Executing polling..."
	
		//Check Messages
		httpPost ([uri: getAPIUrl("messages"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
	if (response.data.object[0] != null) {
			state.messageId = response.data.object[-1].messageId
			state.messageTime = response.data.object[-1].msgTime
			state.message = response.data.object[-1].msg
	if (response.data.object[-1].msgType == 4 && state.oldmessageId != state.messageId) {
			state.oldmessageId = state.messageId
			sendEvent(name: "message", value: "SOS ALERT $state.name $state.sim")
			log.info "Message: SOS ALERT $state.name $state.messageTime $state.sim"
    }
    if (state.oldmessageId != state.messageId) {
			state.oldmessageId = state.messageId			
			sendEvent(name: "message", value: "$state.message $state.sim")
			log.info "Message: $state.message $state.messageTime $state.sim"
		}
	else
			log.info "Current message ID: $state.messageId / Previous Message ID: $state.oldmessageId"
	}
	else
	{
			sendEvent(name: "message", value: none)
			log.info "Message: None"
	}

    //apiLogout()
 }
}

def apiLogin() {
	//Login to the system
    log.info "Executing Login..."
   
   	//Define the login Auth Body and Header Information
	if (settings.region == "US") { state.clientid = "dokiwatch_us_api_android"}
	if (settings.region == "Non-US") { state.clientid = "wherecom_dokiwatch_api_android"}
    def authBody = [ "password":settings.password,
    				"packageName":"com.doki.dokiwatch.us",
					"client_id":state.clientid,
                    "username":settings.username,
					"lang":"en-US",
					"version":"android-56",
					"client_secret":settings.secret,
					"timezone":"EST",
					"grant_type":"password" ]                    
 
    try {
        httpPost([ uri: getAPIUrl("token"), contentType: "application/json; charset=utf-8", body: authBody ]) { response ->
        	state.auth = response.data.object
            state.auth.respAuthHeader = state.auth.token_type + " " + state.auth.access_token
            state.auth.tokenExpiry = now() + state.auth.expires_in
			state.memberId = response.data.object.memberId
            
   	//Define the token Header Information
    state.tokenHeader = [ "Authorization":state.auth.respAuthHeader,
					"app_id":"com.doki.dokiwatch.us",
					"client_id":"dokiwatch_us_api_android",
					"lang":"en-US",
					"timezone":"EST" ] 
        }
 	} catch (e) {
    	//state.token = 
    }
    
    //Check for valid Member ID, and if not get it
    if (!state.memberId)
   	{
    	getmemberId()
   	}
    
    //Check for valid Holder ID, and if not get it
    //Might be able to expand this to multiple systems
    if (!state.holderId)
    {
    	getholderId()
    }
}

def getmemberId() {
	//check auth and get memberId    
    httpGet ([uri: getAPIUrl("memberId"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
        state.memberId = response.data.object.memberId
    }
    log.info "User ID: $state.memberId"
}

def getholderId() {
	//get Holder Info
    httpGet ([uri: getAPIUrl("holderId"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
		state.holderId = response.data.object[0].holderId
		state.name = response.data.object[0].name
		state.sim = response.data.object[0].sim

    }
    log.info "Holder ID: $state.holderId"
    log.info "Holder Name: $state.name"
    log.info "Holder SIM: $state.sim"
}

def checkAuth()
{
	log.info "Checking to see if time has expired...."
        
    //If no State Auth, or now Token Expiry, or time has expired, need to relogin
    //log.info "Expiry time: $state.auth.tokenExpiry"
    if (!state.auth || !state.auth.tokenExpiry || now() > state.auth.tokenExpiry) {    
    	log.info"Token Time has expired, excecuting re-login..."
        apiLogin()
    }
    
	//Check Auth
    try {
        httpGet ([uri: getAPIUrl("memberId"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
            return response.status        
        }
    } catch (e) {
        state.clear()
        apiLogin()
        httpGet ([uri: getAPIUrl("memberId"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
            return response.status        
    }
}
}

def apiLogout() {
			state.clear()
            state.memberId = null
			state.holderId = null
            log.info "Logged out from API."
    }

def getTime()
{
	def tDate = new Date()
    return tDate.getTime()
}

def getAPIUrl(urlType) {
	if (urlType == "token")
    {
    	return "http://api.wherecom.com:8099/umeox/api/oauth2/accessToken"
    }
    else if (urlType == "memberId")
    {
    	return "http://api.wherecom.com:8099/umeox/api/member/get?memberId=$state.memberId"
    }
    else if (urlType == "holderId" )
    {
    	return "http://api.wherecom.com:8099/umeox/api/holder/v3/list?memberId=$state.memberId&currentDate="
    }
    else if (urlType == "messages" )
    {
   		return "http://api.wherecom.com:8099/umeox/api/chat/getInfo?type=0&friendId=$state.holderId&memberId=$state.memberId&messageId="
    } 
    else
    {
    	log.info "Invalid URL type"
    }
}