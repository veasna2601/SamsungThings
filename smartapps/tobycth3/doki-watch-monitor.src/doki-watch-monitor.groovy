/**
 *  Doki Watch Monitor
 *
 *  Copyright 2018 Toby Harris
 *
 *  Author: toby@cth3.com
 *  Date: 7/6/2018
 *
 *  Integrates SmartThings with Doki Watch Messages
 */


// Automatically generated. Make future change here.
definition(
    name: "Doki Watch Monitor",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Integrates SmartThings with Doki Watch Messages",
    category: "Safety & Security",
    iconUrl: "http://cdn.device-icons.smartthings.com/Health & Wellness/health7-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Health & Wellness/health7-icn@2x.png")

preferences {
  section("Monitor these Doki Watches") {
	input "dokiwatch", "capability.Polling", title: "Select Doki Watch", multiple: true
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
	subscribe(dokiwatch, "message", alertmessage)
  }

def alertmessage(evt) {
  log.debug "$evt.displayName: $evt.value"
   state.message = evt.value
   def message = state.message
   log.info(message)
   sendMessage(message)
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