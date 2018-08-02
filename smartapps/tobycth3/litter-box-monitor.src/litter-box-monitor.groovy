/**
 *  Litter Box Monitor
 *
 *  Author: toby@cth3.com
 *  Date: 2/8/17
 *
 * Monitors Litter Robots ready/full light
 */
definition(
    name: "Litter Box Monitor",
    namespace: "tobycth3",
    author: "Toby Harris",
    description: "Monitors Litter Robots ready/full light",
    category: "Pets",
    iconUrl: "http://cdn.device-icons.smartthings.com/Kids/kids9-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Kids/kids9-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Kids/kids9-icn@2x.png",
    oauth: true)


preferences {
  section("Monitor this litter box") {
    input "litterbox", "capability.indicator", title: "Select litter box"
	}

  section("Not ready threshold") {
    input "threshold", "number", title: "Number of minutes"
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
log.info ("state: ${state}")
    if (state.accessToken == null) {
        initAccessToken()
    }
}
  
mappings {
  path("/litterbox/:command") {
    action: [
      PUT: "updatelitterbox"
    ]
  }
}


void updatelitterbox() {
    def command = params.command
    switch(command) {
        case "ready":
		log.debug("Set status to ready")
		state.action = "readyaction"
		readyaction()
            break
        case "notready":
		log.debug("Set status to not ready")
		state.action = "notreadyaction"
		notreadyaction()
            break
        case "full":
		log.debug("Set status to full")
		state.action = "fullaction"
		fullaction()
            break
        default:
            httpError(400, "$command is not a valid command for all switches specified")
    }

}
  
def readyaction() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
     def message = "Litter box is ready"
      log.info(message)
	 // sendMessage(message)
	  state.lastaction = "readyaction"
	  unschedule("notreadytimer")
      litterbox?.ready()
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
}

def notreadyaction() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
     def message = "Litter box is not ready"
      log.info(message)
	 // sendMessage(message)
      state.lastaction = "notreadyaction"
      log.info("Starting not ready timer")
      def delay = threshold * 60
      runIn(delay, notreadytimer)
      litterbox?.notready()
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
}

def fullaction() {
	log.debug("Last action was: $state.lastaction")
	 if (!alreadyRun) {
      def message = "Litter box is full"
      log.info(message)
	  sendMessage(message)
	  state.lastaction = "fullaction"
      litterbox?.full()
  }
  else {
	 if (alreadyRun) {  
     log.debug("Action '${state.action}' already executed")
  }
 }
}

def notreadytimer() {
      def message = "Litter box has been not ready for more than ${threshold} minutes"
      log.info(message)
      sendMessage(message)
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
    if (sendPush) {
		sendPush(msg)
   }
}



private def initAccessToken() {
    try {
        def token = createAccessToken()
        log.info "Created access token: ${token})"
        state.url = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/litterbox/<action>?access_token=${token}"
    } catch (e) {
        log.error "Cannot create access token. ${e}"
        state.url = null
        return false
    }

    return true
}