/**
 *  Switch Monitor
 *
 *  Author: toby@cth3.com
 *  Date: 3/27/17
 *
 *  Monitors a switch, turns it back on if it was turned off during a specified time window
 */


// Automatically generated. Make future change here.
definition(
    name: "Switch Monitor",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Monitors a switch, turns it back on if it was turned off during a specified time window",
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
	
  section(title: "Monitor these switchs") {
    input "switchs", "capability.switch", title: "Select switchs", multiple: true
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
  subscribe(switchs, "switch.off", switchAction)
  }
  
def switchAction(evt) {
  log.info("Starting date/time/mode check")
	if (allOk) {
	log.debug("Date/time and mode conditions match")
	 turnON()
   }
  else {
     log.debug("No condition match for date/time or mode")
   }
  }


def turnON() {
      def message = "Turning switch back on because it was turned off during time window"
      log.info(message)
	  sendMessage(message)
      switchs?.on()
  }

  
// TODO - centralize somehow
private getAllOk() {
	modeOk && daysOk && timeOk
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