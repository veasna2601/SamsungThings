/**
 *  Ceiling Fan Control
 *
 *  Author: toby@cth3.com
 *  Date: 6/1/16
 *  
 * Controls the speeds of ceiling fans based off the temperature swing from a thermostat cooling setpoint.
 */
 
 
definition(
    name: "Ceiling Fan Control",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Controls the speeds of ceiling fans based off the temperature swing from a thermostat cooling setpoint.",
    category: "Green Living",
	iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light24-icn.png", 
   	iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light24-icn@2x.png",
)

preferences {
	section("Monitor this thermostat cooling setpoint") {
		input("thermostat", "capability.thermostat", title: "Select thermostat")
	}
	section("Monitor this temperature sensor"){
		input "tempsensor", "capability.temperatureMeasurement", title: "Select temperature sensor"
	}
	section("Control these ceiling fans"){
		input "fanspeed", "capability.switchLevel", multiple:true, title: "Select ceiling fan"
	}
	section("Temperature swing between fan speeds"){
		input "temperatureswing", "enum", title: "Temperature swing", options: ["1.0","2.0","3.0","4.0","5.0"]
	}
	section("Notifications"){
		input("recipients", "contact", title: "Send notifications to", required: false) {
			input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
			paragraph "If outside the US please make sure to enter the proper country code"
			input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes", "No"]
		}
	}
}

def installed() {
	init()
}

def updated() {
  unsubscribe()
  unschedule()
  updatestate()
  init()
} 

def init() {
	subscribe(thermostat, "coolingSetpoint", setpointaction)
	subscribe(tempsensor, "temperature", temperatureaction)
}

def updatestate() {
	log.info("Updating temperature and setpoint")
	state.temperature = tempsensor.currentValue("temperature").doubleValue()
	state.setpoint = thermostat.currentValue("coolingSetpoint").doubleValue()
	fanaction()	
}

def temperatureaction(evt) {
    log.info "temperatureaction: $evt.displayName - $evt.value"
    state.temperature = evt.doubleValue
	fanaction()
}

def setpointaction(evt) {
    log.info "setpointaction: $evt.displayName - $evt.value"
    state.setpoint = evt.doubleValue
	fanaction()
}

def fanaction() {
	log.debug "(Temperature: $state.temperature, Setpoint: $state.setpoint)"
    def swingthreshold = Double.parseDouble(temperatureswing)
	def temperaturediff = state.temperature - state.setpoint
	def overtemp = state.setpoint - state.temperature

	if (temperaturediff >= swingthreshold*2) {
	log.debug("Set fans to high")
	state.action = "highaction"
	highaction()
  }
  else {
	if (temperaturediff >= swingthreshold*1) {
	log.debug("Set fans to medium")
	state.action = "mediumaction"
	mediumaction()
  }
  else {
	if (temperaturediff >= swingthreshold*0) {
	log.debug("Set fans to low")
	state.action = "lowaction"
	lowaction()
  }
   else {
	if (overtemp >= swingthreshold) {
	log.debug("Set fans to off")
	state.action = "offaction"
	offaction()
     }
    }
   }
  }
 }


def highaction() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
      def message = "Setting ceiling fans to high"
      log.info(message)
	  sendMessage(message)
	  state.lastaction = "highaction"
      fanspeed?.setLevel(90)
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
}

def mediumaction() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
      def message = "Setting ceiling fans to medium"
      log.info(message)
	  sendMessage(message)
	  state.lastaction = "mediumaction"
      fanspeed?.setLevel(60)
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
}

def lowaction() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
      def message = "Setting ceiling fans to low"
      log.info(message)
	  sendMessage(message)
	  state.lastaction = "lowaction"
      fanspeed?.setLevel(30)
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
} 

def offaction() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
      def message = "Setting ceiling fans to off"
      log.info(message)
	  sendMessage(message)
	  state.lastaction = "offaction"
      fanspeed?.off()
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
} 

// TODO - centralize somehow
private getAlreadyRun() {
	def result = false
	if (state.lastaction == state.action) {
	result = true }
	log.trace "alreadyRun = $result"
	result
}

private sendMessage(msg) {
	Map options = [:]
	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients, options)
	} else {
		if (!phone || pushAndPhone != 'No') {
			log.debug 'sending push'
			options.method = 'push'
			//sendPush(msg)
		}
		if (phone) {
			options.phone = phone
			log.debug 'sending SMS'
			//sendSms(phone, msg)
		}
		sendNotification(msg, options)
	}
}