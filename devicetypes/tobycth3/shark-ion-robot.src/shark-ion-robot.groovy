/**
 *  Shark ION Robot
 *
 *  Copyright 2018 Toby Harris
 *
 *  Author: toby@cth3.com
 *  Date: 7/10/2018
 *
 *  Integrates SmartThings with Shark ION Robot API
 */

preferences {
	input(name: "username", type: "text", title: "Username", required: "true", description: "Shark Username")
	input(name: "password", type: "password", title: "Password", required: "true", description: "Shark Password")
	input(name: "secret", type: "password", title: "Client Secret", required: "true", description: "Shark Client Secret")
}

metadata {	
	definition (name: "Shark ION Robot", namespace: "tobycth3", author: "Toby Harris") {
		capability "Battery"
		capability "Polling"
		capability "Switch"
		capability "Refresh"
		capability "Tone"
		
		command "clean"
		command "dock"
		command "spot"
		command "pause"
}
tiles(scale: 2) {
    multiAttributeTile(name:"status", type: "generic", width: 6, height: 4){
        tileAttribute ("device.status", key: "PRIMARY_CONTROL") {

            attributeState "default", label:'unknown', icon: "st.unknown.unknown.unknown", backgroundColor: "#000000"
			attributeState "paused", label:'${currentValue}', icon: "st.Appliances.appliances13", backgroundColor: "#505050"
            attributeState "cleaning", label:'${currentValue}', icon: "st.Appliances.appliances13", backgroundColor: "#008CC1"
			attributeState "docking", label:'${currentValue}', icon: "st.Appliances.appliances13", backgroundColor: "#00BEAC"
            attributeState "charging", label:'${currentValue}', icon: "st.Appliances.appliances13", backgroundColor: "#ffa81e"
			attributeState "charged", label:'${currentValue}', icon: "st.quirky.spotter.quirky-spotter-plugged", backgroundColor: "#79b821"
			attributeState "error", label:'${currentValue}', icon: "st.Appliances.appliances13", backgroundColor: "#d44556"			
        }
		
		tileAttribute("device.battery", key: "SECONDARY_CONTROL", wordWrap: true) {
			attributeState("default", label:'Battery ${currentValue}%')
		}
    }

		standardTile("clean", "device.switch", width: 2, height: 2, inactiveLabel: false, canChangeIcon: false, decoration: "flat") {
			state("pause", label: 'clean', action: "clean", icon: "st.Appliances.appliances13", backgroundColor: "#505050", nextState: "clean")
			state("clean", label: 'clean', action: "pause", icon: "st.Appliances.appliances13", backgroundColor: "#008CC1", nextState: "pause")
			}
	
		standardTile("dock", "device.switch", width: 2, height: 2, inactiveLabel: false, canChangeIcon: false, decoration: "flat") {
			state("pause", label: 'dock', action: "dock", icon: "st.Appliances.appliances13", backgroundColor: "#505050", nextState: "dock")
			state("dock", label: 'dock', action: "pause", icon: "st.Appliances.appliances13", backgroundColor: "#008CC1", nextState: "pause")
			}
			
		standardTile("spot", "device.switch", width: 2, height: 2, inactiveLabel: false, canChangeIcon: false, decoration: "flat") {
			state("pause", label: 'spot', action: "spot", icon: "st.Appliances.appliances13", backgroundColor: "#505050", nextState: "spot")
			state("spot", label: 'spot', action: "pause", icon: "st.Appliances.appliances13", backgroundColor: "#008CC1", nextState: "pause")
			}
			
		standardTile("beep", "device.beep", width: 2, height: 2, inactiveLabel: false, canChangeIcon: false, decoration: "flat") {
			state ("beep", label:'beep', action:"tone.beep", icon:"st.quirky.spotter.quirky-spotter-sound-on", backgroundColor:"#ffffff")
		}
		
		standardTile("connection", "device.connection", width: 2, height: 2, inactiveLabel: false, canChangeIcon: false, decoration: "flat") {
			state ("Online", label:'${currentValue}', icon: "st.Appliances.appliances13", backgroundColor: "#79b821")
			state ("Offline", label:'${currentValue}', icon: "st.Appliances.appliances13", backgroundColor: "#bc2323")
		}

		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, canChangeIcon: false, decoration: "flat") {
			state("default", label:'refresh', action:"poll", icon:"st.secondary.refresh-icon")
		}
			
		main(["status"])
		details(["status","clean","dock","spot","beep","connection","refresh"])
	}
}

def installed() {
  init()
}

def updated() {
  log.info "Removing Schedule"
  unschedule()
  init()
}
  
def init() {
	log.info "Setting Schedule (every 1 hour)"
	runEvery1Hour(poll)
	poll()
}

// handle commands
def on() {
	clean()
}

def off() {
	dock()
}

def pause() {
	log.info "Sending command 'Pause'"
	setState ('pause')
}

def spot() { 
	log.info "Sending command 'Spot'"
	setState ('spot')
}

def clean() {
	log.info "Sending command 'Clean'"
	setState ('clean')
}

def dock() {
	log.info "Sending command 'Dock'"
	setState ('dock')
}

def beep() {
	log.info "Sending command 'Beep'"
	setState ('beep')
}

def setState (command){
	//Check Auth first
	checkAuth()
    def timeout = false;
    
    if (command == "pause")
    {
    	try {
			def operation=[
			value:0]
	
			def operationData = [ datapoint:operation ]
			def operationBuilder = new groovy.json.JsonBuilder(operationData)
			def operationBody = operationBuilder.toString()

			httpPostJson([ uri: getAPIUrl("operation"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8", body: operationBody ])	
        } catch (e) {
        	timeout = true;
        	log.debug "SET PAUSE ERROR: $e"
        }
    }
    else if (command == "spot")
    {
    	try {
			def operation=[
			value:1]
	
			def operationData = [ datapoint:operation ]
			def operationBuilder = new groovy.json.JsonBuilder(operationData)
			def operationBody = operationBuilder.toString()

			httpPostJson([ uri: getAPIUrl("operation"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8", body: operationBody ])	
        } catch (e) {
        	timeout = true;
        	log.debug "SET SPOT ERROR: $e"
        }
    }
    else if (command == "clean")
    {
    	try {
			def operation=[
			value:2]
	
			def operationData = [ datapoint:operation ]
			def operationBuilder = new groovy.json.JsonBuilder(operationData)
			def operationBody = operationBuilder.toString()

			httpPostJson([ uri: getAPIUrl("operation"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8", body: operationBody ])			
        } catch (e) {
        	timeout = true;
        	log.debug "SET CLEAN ERROR: $e"
        }
    }
    else if (command == "dock")
    {
    	try {
			def operation=[
			value:3]
	
			def operationData = [ datapoint:operation ]
			def operationBuilder = new groovy.json.JsonBuilder(operationData)
			def operationBody = operationBuilder.toString()

			httpPostJson([ uri: getAPIUrl("operation"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8", body: operationBody ])
        } catch (e) {
        	timeout = true;
        	log.debug "SET DOCK ERROR: $e"
        }
    }
    else if (command == "beep")
    {
    	try {
			def operation=[
			value:1]
	
			def operationData = [ datapoint:operation ]
			def operationBuilder = new groovy.json.JsonBuilder(operationData)
			def operationBody = operationBuilder.toString()

			httpPostJson([ uri: getAPIUrl("beep"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8", body: operationBody ])	
        } catch (e) {
        	timeout = true;
        	log.debug "SET BEEP ERROR: $e"
        }
    }
    else
    {
        log.info "Invalid state requested."
    }
    
    //If not a timeout, we can poll in 5 seconds
    if (!timeout) {
    	runIn(5, poll)
    } else {
    	//There was a timeout, so we can't poll right away. Wait 10 seconds and try polling.
    	runIn(10, poll)
    }
}

def poll() {
	//Check Auth first
	checkAuth()

    log.info "Executing polling"
   
	httpGet ([uri: getAPIUrl("operatingStatus"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
        
		//Get operating mode
		log.debug "Operating Mode: $response.data.property.value"
		state.mode = response.data.property.value
		if  (state.mode[0] == 0) {
			sendEvent(name: "switch", value: "pause")
			}
		if  (state.mode[0] == 1) {
			sendEvent(name: "switch", value: "spot")
			}
		if  (state.mode[0] == 2) {
			sendEvent(name: "switch", value: "clean")
			}
		if (state.mode[0] == 3) {
			sendEvent(name: "switch", value: "dock")
			}
    }
	
	httpGet ([uri: getAPIUrl("batteryStatus"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
        
		//Get battery status
		log.debug "Battery status: $response.data.property.value"
		if  (response.data.property.value != null) {
			state.battery = response.data.property.value
			sendEvent(name: "battery", value: state.battery[0])
			}
    }
	
	httpGet ([uri: getAPIUrl("chargingStatus"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
        
		//Get charging status
		log.debug "Charging status: $response.data.property.value"
		state.charging = response.data.property.value
	}
	
	httpGet ([uri: getAPIUrl("errorStatus"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
        
		//Get error status
		log.debug "Error status: $response.data.property.value"
		state.error = response.data.property.value
	}
			
		//Update status
		if  (state.connection == "Online") {
			sendEvent(name: "connection", value: "Online")
			}
		else if (state.connection != "Online")
			{
			sendEvent(name: "connection", value: "Offline")
			}
		if (state.error[0] == 0) {
			if  (state.mode[0] == 0) {
				sendEvent(name: "status", value: "paused")
				}
			if  (state.mode[0] == 1 || state.mode[0] == 2) {
				sendEvent(name: "status", value: "cleaning")
				log.info "Modifying Schedule (every 5 minutes)"
				runIn(5*60, poll)
				}
			if  (state.mode[0] == 3 && state.charging[0] == 0) {
				sendEvent(name: "status", value: "docking")
				log.info "Modifying Schedule (every 5 minutes)"
				runIn(5*60, poll)
				}
			if  (state.mode[0] == 3 && state.charging[0] == 1 && state.battery[0] < 100) {
				sendEvent(name: "status", value: "charging")
				}
			if  (state.mode[0] == 3 && state.charging[0] == 1 && state.battery[0] == 100) {
				sendEvent(name: "status", value: "charged")
				}
			}
			else if (state.error[0] != 0)
			{
			sendEvent(name: "status", value: "error")
			}
	
    apiLogout()
 }

def apiLogin() {
	//Login to the system
    log.info "Executing Login"
   
   	//Define the login Auth Body and Header Information
	
	def authUser=[
    email:settings.username,
	application:[app_id:"Shark-iOS-field-id",
    app_secret:settings.secret,],	
    password:settings.password,]
	
	def authData = [ user:authUser ]
	def authBuilder = new groovy.json.JsonBuilder(authData)
	def authBody = authBuilder.toString()

    try {
        httpPostJson([ uri: getAPIUrl("token"), contentType: "application/json; charset=utf-8", body: authBody ]) { response ->
        	state.auth = response.data
            state.auth.respAuthHeader = "auth_token " + state.auth.access_token
            state.auth.tokenExpiry = now() + state.auth.expires_in
			state.DSN = response.data.DSN
      
   	//Define the token Header Information
    state.tokenHeader = [ "Authorization": state.auth.respAuthHeader ]
        }
 	} catch (e) {
    log.warn "Login Failed"
		log.trace "$state.auth"
    	//state.token = 
    }
    
    //Check for valid DSN ID, and if not get it
    if (!state.DSN)
   	{
    	getDSN()
   	}
    
    //Check for valid UUID, and if not get it
    //Might be able to expand this to multiple systems
    if (!state.UUID)
    {
    	getUUDI()
    }
}

def getDSN() {
	//check auth and get DSN  
    httpGet ([uri: getAPIUrl("DSN"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
		state.DSN = response.data.device.dsn[0]
		state.name = response.data.device.product_name[0]
		state.connection = response.data.device.connection_status[0]
    }
    log.debug "DSN: $state.DSN"
    log.debug "Name: $state.name"
    log.debug "Connection: $state.connection"
}

def getUUDI() {
	//get UUID Info
    httpGet ([uri: getAPIUrl("UUID"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
		state.UUID = response.data.uuid

    }
    log.debug "UUID: $state.UUID"
}

def checkAuth()
{
	log.info "Checking to see if token has expired."
        
    //If no State Auth, or now Token Expiry, or time has expired, need to relogin
    //log.debug "Expiry time: $state.auth.tokenExpiry"
    if (!state.auth || !state.auth.tokenExpiry || now() > state.auth.tokenExpiry) {    
    	log.info"Token has expired, excecuting re-login"
        apiLogin()
    }
    
	//Check Auth
    try {
        httpGet ([uri: getAPIUrl("DSN"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
            return response.status        
        }
    } catch (e) {
        state.clear()
        apiLogin()
        httpGet ([uri: getAPIUrl("DSN"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8"]) { response ->
            return response.status        
    }
}
}

def apiLogout() {
	def logoutUser=[access_token:state.auth.access_token]
	
	def logoutData = [ user:logoutUser ]
	def logoutBuilder = new groovy.json.JsonBuilder(logoutData)
	def logoutBody = logoutBuilder.toString()
	
    httpPostJson([ uri: getAPIUrl("logout"), headers: state.tokenHeader, contentType: "application/json; charset=utf-8", body: logoutBody ]) { response ->
        if (response.status == 200) {
			state.clear()
            log.info "Logged out from API"
        }
    }
}

def getTime()
{
	def tDate = new Date()
    return tDate.getTime()
}

def getAPIUrl(urlType) {
	if (urlType == "token")
    {
    	return "https://user-field.aylanetworks.com/users/sign_in.json"
    }
    else if (urlType == "DSN")
    {
    	return "https://ads-field.aylanetworks.com/apiv1/devices.json"
    }
    else if (urlType == "UUID" )
    {
    	return "https://user-field.aylanetworks.com/users/get_user_profile.json"
    }
    else if (urlType == "logout" )
    {
   		return "https://user-field.aylanetworks.com/users/sign_out.json"
    } 
    else if (urlType == "operation" )
    {
   		return "https://ads-field.aylanetworks.com/apiv1/properties/63548468/datapoints.json"
    } 
    else if (urlType == "beep" )
    {
   		return "https://ads-field.aylanetworks.com/apiv1/properties/63548438/datapoints.json"
    } 
    else if (urlType == "operatingStatus" )
    {
   		return "https://ads-field.aylanetworks.com/apiv1/dsns/$state.DSN/properties.json?names%5B%5D=GET_Operating_Mode"
    } 
    else if (urlType == "batteryStatus" )
    {
   		return "https://ads-field.aylanetworks.com/apiv1/dsns/$state.DSN/properties.json?names%5B%5D=GET_Battery_Capacity"
    } 
    else if (urlType == "chargingStatus" )
    {
   		return "https://ads-field.aylanetworks.com/apiv1/dsns/$state.DSN/properties.json?names%5B%5D=GET_Charging_Status"
    } 
    else if (urlType == "errorStatus" )
    {
   		return "https://ads-field.aylanetworks.com/apiv1/dsns/$state.DSN/properties.json?names%5B%5D=GET_Error_Code"
    } 
    else
    {
    	log.debug "Invalid URL type"
    }
}