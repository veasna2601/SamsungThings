/**
 *  Lock Control
 *
 *  Author: toby@cth3.com
 *  Date: 7/18/18
 *
 *  Monitors a set of door locks and automatically relocks if any are unlocked too long.
 */
 
definition(
    name: "Lock Control",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Monitors a set of door locks and automatically relocks if any are unlocked too long.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Home/home3-icn.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Home/home3-icn@2x.png")

preferences {
  
  section("Monitor and auto-lock these door locks") {
	input "doorlock", "capability.lock", title: "Select locks", multiple: true
  }
  
  section("Auto-lock door sensors") {
	input "locksensor", "capability.contactSensor", title: "Select contact sensors", multiple: true
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
    subscribe(doorlock, "lock", doorlockstatus)
	subscribe(locksensor, "contact", locksensorstatus)
  }

  def doorlockcheck() {
      def message = "${state.lock} has been unlocked for more than ${threshold} minutes"
      log.info(message)
      sendMessage(message)
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
   doorlock?.poll()
  }
    else {
  unschedule("autolockcheck")
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