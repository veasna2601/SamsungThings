/**
 *  Camera Control
 *
 *  Author: toby@cth3.com
 *  Date: 7/28/2017
 *
 *  Controls FTP recording on cameras based on Smartthings location mode, contact sensors and door locks.
 */


// Automatically generated. Make future change here.
definition(
    name: "Camera Control",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Controls FTP recording on cameras based on Smartthings location mode, contact sensors and door locks.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Entertainment/entertainment9-icn.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Entertainment/entertainment9-icn@2x.png")

preferences {
  section("Control these cameras") {
    input "camera", "capability.switch", title: "Select cameras", multiple: true
  }
  
  section("Monitor these alarm states") {
    input "alarmsystem", "enum", title: "Select on state", multiple: true, required: false, metadata:[values:["off", "away", "stay"]]
  }
  
  section("Monitor these contact sensors") {
	input "contactsensor", "capability.contactSensor", title: "Select contact sensors", multiple: true
  }
  
  section("Monitor these door locks") {
	input "doorlock", "capability.lock", title: "Select door locks", multiple: true
  } 

  section("Enable recording when location mode matches") {
    input "modes", "mode", title: "Select enable mode", multiple: true
  }
  
  section("Disable recording delay") {
    input "delay", "number", defaultValue: "15", title: "Number of minutes"
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
  subscribe(location, modeAction)
  subscribe(location, "alarmSystemStatus", shmAction)
  subscribe(contactsensor, "contact", sensorAction)
  subscribe(doorlock, "lock", lockAction)  
  
  }

  
def modeAction(evt) {
 log.info "$evt.displayName: $evt.value"
  if(evt.value in modes) {
    log.debug("Mode matches enable recording")
	state.action = "recordEnable"
	 unschedule("recordDisable")
	 state.reason = "'${evt.displayName}' is '${evt.value}'"
	 recordEnable()
  }
 else {
  if(!(evt.value in modes)) {
    log.debug("Mode matches disable recording")
	log.debug "Scheduling recordDisable in $delay minutes"
	state.action = "recordDisable"
	 state.reason = "'${evt.displayName}' is '${evt.value}'" 
	 runIn(delay * 60, 'recordDisable')	
  }
  else {
    log.debug("No condition match for location mode '${location.mode}'")
	state.action = "none"
    }
   }  
  }


def shmAction(evt) {
 log.info "$evt.displayName: $evt.value"
  if(evt.value in alarmsystem) {
    log.debug("Alarm matches enable recording")
	state.action = "recordEnable"
	 unschedule("recordDisable")
	 state.reason = "'${evt.displayName}' is '${evt.value}'"
	 recordEnable()
  }
 else {
  if(!(evt.value in alarmsystem)) {
    log.debug("Alarm matches disable recording")
	log.debug "Scheduling recordDisable in $delay minutes"
	state.action = "recordDisable"
	 state.reason = "'${evt.displayName}' is '${evt.value}'"	 
	 runIn(delay * 60, 'recordDisable')	
  }
  else {
    log.debug("No condition match for alarm '${evt.displayName}' '${evt.value}'")
	state.action = "none"
    }
   }  
  }


def sensorAction(evt) {
 log.info "$evt.displayName: $evt.value"
  if(evt.value != "closed") {
    log.debug("Contact matches enable recording")
	state.action = "recordEnable"
	 unschedule("recordDisable")
	 state.reason = "'${evt.displayName}' is '${evt.value}'"
	 recordEnable()
  }
 else {
  if(evt.value == "closed") {
    log.debug("Contact matches disable recording")
	log.debug "Scheduling recordDisable in $delay minutes"
	state.action = "recordDisable"
	 state.reason = "'${evt.displayName}' is '${evt.value}'"	 
	 runIn(delay * 60, 'recordDisable')	
  }
  else {
    log.debug("No condition match for contact '${evt.displayName}' '${evt.value}'")
	state.action = "none"
    }
   }  
  }
  
  
def lockAction(evt) {
 log.info "$evt.displayName: $evt.value"
  if(evt.value != "locked") {
    log.debug("Lock matches enable recording")
	state.action = "recordEnable"
	 unschedule("recordDisable")
	 state.reason = "'${evt.displayName}' is '${evt.value}'"
	 recordEnable()
  }
 else {
  if(evt.value == "locked") {
    log.debug("Lock matches disable recording")
	log.debug "Scheduling recordDisable in $delay minutes"
	state.action = "recordDisable"
	 state.reason = "'${evt.displayName}' is '${evt.value}'"	 
	 runIn(delay * 60, 'recordDisable')	
  }
  else {
    log.debug("No condition match for lock '${evt.displayName}' '${evt.value}'")
	state.action = "none"
    }
   }  
  }
 
 
def recordEnable() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
      def message = "Enabling camera recording because ${state.reason}"
      log.info(message)
      sendMessage(message)
	  state.lastaction = "recordEnable"
      camera?.on()
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
}


def recordDisable() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun && allOk) {
      def message = "Disabling camera recording because ${state.reason}"
      log.info(message)
      sendMessage(message)
	  state.lastaction = "recordDisable"
      camera?.off()
  } 
  else {
	 if (alreadyRun || !allOk) {  
     log.debug("No condition match for already run: '${alreadyRun}', mode: '${modeOk}', alarm: '${alarmOk}', contacts: '${allClosed}' or locks: '${allLocked}'")
  }
 }
}


private getAllOk() {
	modeOk && alarmOk && allClosed && allLocked
}

private getAlreadyRun() {
	def result = false
	if (state.lastaction == state.action) {
	result = true }
	log.trace "alreadyRun = $result"
	result
}
 
private getModeOk() {
	def result = !modes || !modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getAlarmOk() {
	def shmstate = location.currentState("alarmSystemStatus").value.toLowerCase()
	def result = !alarmsystem || !alarmsystem.contains(shmstate)
	log.trace "alarmOk = $result"
	result
} 
  
private getAllClosed() {
	def result = false
	if (contactsensor.find() { it.currentValue('contact') != 'closed'; } == null) {
	result = true }
    log.trace "allClosed = $result"
	result
}

private getAllLocked() {
	def result = false
	if (doorlock.find() { it.currentValue('lock') != 'locked'; } == null) {
	result = true }
    log.trace "allLocked = $result"
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