/**
 *  Door Monitor
 *
 *  Author: toby@cth3.com
 *  Date: 3/7/18
 *
 *  Monitors a set of door sensors / door locks and sends a notification if any are open / unlocked too long.
 *  Sends a notification if a door is opened / unlocked based on Smartthings location mode. 
 */


// Automatically generated. Make future change here.
definition(
    name: "Door Monitor",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Monitors a set of door sensors / door locks and sends a notification if any are open / unlocked too long. Sends a notification if a door is opened / unlocked based on Smartthings location mode. ",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/doors/garage/garage-closed.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/doors/garage/garage-closed@2x.png")

preferences {
  section("Monitor these door sensors") {
	input "doorsensor", "capability.contactSensor", title: "Select doors", multiple: true
  }
  
  section("Monitor and auto-lock these door locks") {
	input "doorlock", "capability.lock", title: "Select locks", multiple: true
  }
  
  section("Auto-lock door sensors") {
	input "locksensor", "capability.contactSensor", title: "Select contact sensors", multiple: true
  }
  
  section("Alert if opened / unlocked when location mode matches") {
	input "modealert", "mode", title: "Select alert mode", multiple: true
  } 
  
  section("Door open / unlocked threshold") {
    input "threshold", "number", title: "Number of minutes"
  }
  
  section("Auto-lock threshold") {
    input "lockthreshold", "number", title: "Number of minutes"
  }
  
  section("Push Notification?") {
    input "sendPush", "bool", required: false,
    title: "Send Push Notifications?"
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
	subscribe(doorsensor, "contact", doorsensorstatus)
    subscribe(doorlock, "lock", doorlockstatus)
	subscribe(locksensor, "contact", locksensorstatus)
	subscribe(location)
  }

def doorstatuscheck() {
      def message = "${state.sensor} has been open for more than ${threshold} minutes"
      log.info(message)
      sendMessage(message)
  }  
  
def doorlockcheck() {
      def message = "${state.lock} has been unlocked for more than ${threshold} minutes"
      log.info(message)
      sendMessage(message)
  }
  
def doorsensorstatus(evt) {
  log.debug "doorsensorstatus, $evt.displayName: $evt.value"
   state.sensor = evt.displayName
   state.doorstatus = evt.value
  if (evt.value != "closed") {
   if (evt.value != "open") {
   def message = "WARNING: ${state.sensor} has jammed, please check door."
   log.info(message)
   sendMessage(message)
   }
   if (location.mode in modealert) {
   def message = "ALERT! ${state.sensor} was opened while mode is '${location.mode}'"
   log.info(message)
   sendMessage(message)
   }
   log.debug "Starting door check timer"
   def doordelay = threshold * 60
   runIn(doordelay, doorstatuscheck)
  }
  else {
  unschedule("doorstatuscheck")
   }
  }

def doorlockstatus(evt) {
  log.debug "doorlockstatus, $evt.displayName: $evt.value"
   state.lock = evt.displayName
   state.lockstatus = evt.value
  if (evt.value != "locked") {
   if (evt.value != "unlocked") {
   def message = "WARNING: ${state.lock} has jammed, please check door."
   log.info(message)
   sendMessage(message)
   }
   if (location.mode in modealert) {
   def message = "ALERT! ${state.lock} was unlocked while mode is '${location.mode}'"
   log.info(message)
   sendMessage(message)
   }
   log.debug "Starting lock check timer"
   def lockdelay = threshold * 60
   runIn(lockdelay, doorlockcheck)
   
   log.debug "Starting auto-lock timer"
   def autolockdelay = lockthreshold * 60
   runIn(autolockdelay, autolockcheck)
  }
  else {
  unschedule("doorlockcheck")
  unschedule("autolockcheck")
   }
  }
  
  
def locksensorstatus(evt) {
  log.debug "locksensorstatus, $evt.displayName: $evt.value"
   state.locksensor = evt.displayName
   state.locksensorstatus = evt.value
  if (evt.value == "closed") {
   log.debug "Starting auto-lock timer"
   def autolockdelay = lockthreshold * 60
   runIn(autolockdelay, autolockcheck)
  }
    else {
  unschedule("autolockcheck")
  //doorlock?.unlockHack()
   }
  }  
   
def autolockcheck(){   
  if (state.locksensorstatus == "closed" 
  && state.lockstatus != "locked"
  && state.lockstatus != "unknown") {
   log.info "Executing auto-lock"
   doorlock?.lock()
   }
    else {
   log.warn "Aborting - ${state.locksensor} = ${state.locksensorstatus} and ${state.lock} = ${state.lockstatus}"
   }
  }
  
private sendMessage(msg) {
	Map options = [:]
    if (sendPush) {
		sendPush(msg)
   }
}