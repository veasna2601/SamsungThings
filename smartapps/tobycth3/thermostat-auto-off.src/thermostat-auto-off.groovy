/**
 *  Thermostat Auto Off
 *
 *  Author: dianoga7@3dgo.net
 *  Date: 2013-07-21
 *  
 * Modifications by Toby Harris - 3/15/2016
 */

// Automatically generated. Make future change here.
definition(
    name: "Thermostat Auto Off",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Automatically turn off thermostat when windows/doors open. Turn it back on when everything has closed.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Home/home1-icn.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Home/home1-icn@2x.png",
    oauth: true
)

preferences {
	section("Control this thermostat") {
		input("thermostat", "capability.thermostat", title: "Select thermostat")
	}
    
    section("Monitor these door sensors") {
    	input("doorsensor", "capability.contactSensor", title: "Door sensors", multiple: true)
    }
	
	section("Door open/closed delay") {
    	input "delay", "number", title: "Number of minutes"
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
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	state.changed = "turnOn"
	state.scheduled = "turnOn"
	subscribe(doorsensor, 'contact', "sensorChange")
}

def sensorChange(evt) {
	log.debug "$evt.displayName, $evt.value , $state"
    state.sensor = evt.displayName
    if(evt.value == 'open') {
    	log.info "Something is open"
        if (state.scheduled == "turnOn") {
			log.debug "Unscheduling turnOn"
			unschedule('turnOn')
			}
		if (state.changed == "turnOn") {
			log.debug "Scheduling turnOff in $delay minutes"
			state.scheduled = "turnOff"
			runIn(delay * 60, 'turnOff')
			}
    } else if(evt.value == 'closed') {        
        if(!isOpen()) {
        	log.info "Everything is closed"
			if (state.scheduled == "turnOff") {
				log.debug "Unscheduling turnOff"
				unschedule('turnOff')
				}
			if (state.changed == "turnOff") {
				log.debug "Scheduling turnOn in $delay minutes"
				state.scheduled = "turnOn"
				runIn(delay * 60, 'turnOn')
				}
        } else {
        	log.debug "Aborting - something is still open."
        }
    }
}

def isOpen() {
	def result = doorsensor.find() { it.currentValue('contact') == 'open'; }
    log.debug "isOpen results: $result"
    
    return result
}

def turnOff() {
	log.info "Preparing to turn off thermostat due to contact open"
    if(isOpen()) {
      log.debug "Something is still open. Turning thermostat off."
      def message = "Setting ${thermostat} to off because ${state.sensor} is open"
      log.info(message)
      sendMessage(message)
        state.thermostatMode = thermostat.currentValue("thermostatMode")
        state.changed = "turnOff"
    	thermostat.off()
    	log.debug "State: $state"
    } else {
    	log.debug "Just kidding. The platform did something bad."
    }
}

def turnOn() {
    log.info "Setting thermostat to $state.thermostatMode"
    def message = "Setting ${thermostat} to ${state.thermostatMode} because ${state.sensor} has closed"
    log.info(message)
    sendMessage(message)
      thermostat.setThermostatMode(state.thermostatMode)
      state.changed = "turnOn"
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