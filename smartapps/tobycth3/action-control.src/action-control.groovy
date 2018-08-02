/**
 *  Action Control
 *
 *  Author: toby@cth3.com
 *  Date: 5/13/16
 *
 *  Monitors a set of light sensors and executes 'Hello, Home' Action based on ambient light, mode and time of day.
 */
 
  
// Automatically generated. Make future change here.
definition(
    name: "Action Control",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Monitors a set of light sensors and executes 'Hello, Home' Action based on ambient light, mode and time of day.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Home/home4-icn.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Home/home4-icn@2x.png")

preferences {
  page (name: "mainPage")
  page(name: "timeIntervalInput", title: "Only perform dark action during") {
		section {
			input "starting", "time", title: "Start time", required: false
			input "ending", "time", title: "End time", required: false
	  }
	}
  }
  
def mainPage() {
    dynamicPage(name: "mainPage", install: true, uninstall: true) {
	
  def phrases = location.helloHome?.getPhrases()*.label
   if (phrases) {
   phrases.sort()  
  section("Execute action when ambient light matches") {
	input "actionbright", "enum", title: "Select bright action", options: phrases
	input "actiondark", "enum", title: "Select dark action", options: phrases
     }
    }
	
  section(title: "Monitor this light sensor") {
    input "lightsensor", "capability.illuminanceMeasurement", title: "Select light sensor", multiple: true
    input "lightthreshold", "decimal", title: "Illuminance level"
  }

	section("More options", hideable: true, hidden: true) {
    href "timeIntervalInput", title: "Perform dark action only during this time window", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
    input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            }
			
	section("Notifications"){
		input("recipients", "contact", title: "Send notifications to", required: false) {
			input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
			paragraph "If outside the US please make sure to enter the proper country code"
			input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes", "No"]
		}
	}

		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
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
  subscribe(location, modeaction)
  subscribe(lightsensor, "illuminance", brightnessaction)
  }

def brightnessaction(evt) {
	log.info "Current ambient light: $evt.displayName - $evt.integerValue"
	state.illuminance = evt.integerValue
	state.lastlightlevel = state.lightlevel
    
	if (state.illuminance > lightthreshold && !darkTime) {
	log.debug("Ambient light is bright")
	state.lightlevel = "bright"
	if (state.lastlightlevel != state.lightlevel) {
	modeaction()
	}
  }
  else {
	if (state.illuminance <= lightthreshold && darkTime) {
    log.debug("Ambient light is dark")
	state.lightlevel = "dark"
	if (state.lastlightlevel != state.lightlevel) {
	modeaction()
	}
  }
   else {
     log.debug("No condition match for ambient light: '${state.illuminance}' or time window: '${darkTime}'")
    }
   }
  }

def modeaction(evt) {
	log.info "Location mode: $location.mode"
	 if (!modeOk) {  
     log.debug("No actions set for location mode '${location.mode}'")
	 state.lastaction = "none"
	 } 	
  else {	
	if (state.lightlevel == "bright" && modeOk) {
	log.debug("Ambient light is $state.lightlevel")
	def delaytime = new Date(new Date().time + 10000)
	state.action = "brightaction"
	 if (!alreadyRun) {
	runOnce(delaytime, brightaction)
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
}
  else {
	if (state.lightlevel == "dark" && modeOk && darkTime) {
    log.debug("Ambient light is $state.lightlevel")
	def delaytime = new Date(new Date().time + 10000)
	state.action = "darkaction"
	 if (!alreadyRun) {
	runOnce(delaytime, darkaction)	
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
}
  else {
     log.debug("No condition match for ambient light: '${state.lightlevel}', already run: '${alreadyRun}' or mode: '${modeOk}'")
     }
	}
   }
  }


def brightaction() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
      def message = "Performing '${actionbright}' because ambient light is bright"
      log.info(message)
	  sendMessage(message)
	  state.lastaction = "brightaction"
      location.helloHome.execute(actionbright)
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
}

def darkaction() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
      def message = "Performing '${actiondark}' because ambient light is dark"
      log.info(message)
	  sendMessage(message)
	  state.lastaction = "darkaction"
      location.helloHome.execute(actiondark)
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

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDarkTime() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "darkTime = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private getTimeLabel()
{
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