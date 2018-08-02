/**
 *  Light Schedule
 *
 *  Author: toby@cth3.com
 *  Date: 5/18/18
 *
 *  Turns on lights daily based on mode, time of day and day of week. 
 */
 
  
// Automatically generated. Make future change here.
definition(
    name: "Light Schedule",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Turns on lights daily based on mode, time of day and day of week.",
    category: "Mode Magic",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png")

preferences {
  page (name: "mainPage")
  page(name: "timeIntervalInput", title: "Only during this time window") {
		section {
			input "starting", "time", title: "Start time", required: false
			input "ending", "time", title: "End time", required: false
	  }
	}
  }
  
def mainPage() {
    dynamicPage(name: "mainPage", install: true, uninstall: true) {
	
  section(title: "TUrn on these lights") {
    input "lights", "capability.switch", title: "Select lights", multiple: true
  }

  section (title: "Only during this time window") {
	input "starting", "time", title: "Start time", required: false
	input "ending", "time", title: "End time", required: false
    }  

  section(title: "More options") {
	input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
    input "modes", "mode", title: "Only when mode is", multiple: true, required: false
    }

	section("Notifications"){
		input("recipients", "contact", title: "Send notifications to", required: false) {
			input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
			paragraph "If outside the US please make sure to enter the proper country code"
			input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes", "No"]
		}
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
  modeaction()
  subscribe(location, modeaction)
  schedule("0 0 0 * * ?", "actionreset")
  }
  
def actionreset() {
  log.info "Resetting action state"
	if (state.lastaction != "none") {
	 state.lastaction = "none"
   }
  else {
     log.debug("Action '${state.action}' already reset")
   }
 }
 
def modeaction() {
  log.info("Starting mode check")
	if (modeOk) {
	log.debug("Mode conditions match")
	runEvery15Minutes("datetimecheck")
   }
  else {
     log.debug("No condition match for mode")
   }
  }
 
def datetimecheck() {
  log.info("Starting date/time check")
	if (allOk) {
	log.debug("Date/time and mode conditions match")
	state.action = "lightson"
	 lightson()
   }
  else {
     log.debug("No condition match for date/time or mode")
   }
  }


def lightson() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
      def message = "Turning lights on as scheduled"
      log.info(message)
	  sendMessage(message)
	  state.lastaction = "lightson"
      lights?.on()
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
}
  
// TODO - centralize somehow
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getAlreadyRun() {
	def result = false
	if (state.lastaction == state.action) {
	result = true }
	log.trace "alreadyRun = $result"
	result
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
// TODO - End Centralize  
  
  
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