/**
 *  Nest Control
 *
 *  Author: toby@cth3.com
 *  Date: 4/5/16
 *
 *  Changes your Nest thermostat presence based on Smartthings location mode events.
 *  Works in conjunction with Smart Nest by Dianoga.
 */


// Automatically generated. Make future change here.
definition(
    name: "Nest Control",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Changes your Nest thermostat presence based on Smartthings location mode events. Works in conjunction with Smart Nest by Dianoga.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Home/home1-icn.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Home/home1-icn@2x.png")

preferences {
  section("Control this Nest thermostat") {
    input "thermostat", "capability.thermostat", title: "Select thermostat"
  }
  
  section("Set Nest to 'Present' when location mode matches") {
    input "modehome", "mode", title: "Select home mode", multiple: true
  }
  
  section("Set Nest to 'Away' when location mode matches") {
    input "modeaway", "mode", title: "Select away mode", multiple: true
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
  init()
  }
  
def init() {
  subscribe(location, thermostatmode)
  subscribe(thermostat)
  }

def thermostatmode(evt) {
  if(evt.value in modehome && thermostat.currentpresence != "present") {
    log.debug("Nest presence: $thermostat.currentpresence")
     thermostatmodehome()
  }
 else {
  if(evt.value in modeaway && thermostat.currentpresence != "away") {
    log.debug("Nest presence: $thermostat.currentpresence")
     thermostatmodeaway()
  }
  else {
    log.debug("Thermostat presence is already set to '${thermostat.currentpresence}' - aborting")
    }
   }  
  }

def thermostatmodehome() {
      def message = "Setting Nest to 'Present' because mode changed to '${location.mode}'"
      log.info(message)
      sendMessage(message)
      thermostat.present()
  }
  
def thermostatmodeaway() {
      def message = "Setting Nest to 'Away' because mode changed to '${location.mode}'"
      log.info(message)
      sendMessage(message)
      thermostat.away()

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