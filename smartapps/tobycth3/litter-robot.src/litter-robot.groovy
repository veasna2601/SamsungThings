/**
 *  Litter Robot
 *
 *  Copyright 2017 Toby Harris
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Litter Robot", namespace: "tobycth3", author: "Toby Harris") {
		capability "Indicator"
		command "ready"
		command "notready"
		command "full"
		command "resetcount"
	}


	simulator {
		// TODO: define status and reply messages here
	}

tiles(scale: 2) {
    multiAttributeTile(name:"litterbox", type: "generic", width: 6, height: 4){
        tileAttribute ("device.litterbox", key: "PRIMARY_CONTROL") {
            attributeState "ready", label:"Ready", icon: "st.illuminance.illuminance.bright", backgroundColor: "#008CC1"
            attributeState "notready", label:"Not Ready", icon: "st.illuminance.illuminance.dark", backgroundColor: "#505050"
			attributeState "full", label:"Bin Full", icon: "st.illuminance.illuminance.light", backgroundColor: "#d44556"
        }
		
		tileAttribute("device.count", key: "SECONDARY_CONTROL", wordWrap: true) {
			attributeState("default", label:'${currentValue}')
		}
    }
    
		standardTile("resetcount", "device.resetcount", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'Reset Counter', action:"resetcount", icon:"st.custom.buttons.subtract-icon"
		}

		main(["litterbox"])
		details(["litterbox", "resetcount"])
	}
}


// handle commands
def ready() {
	log.info "Executing 'ready'"
	if (state.status == "full") {
	resetcount()
   }
  else {
	count()
   }
    state.status = "ready"
	sendEvent(name: 'litterbox', value: 'ready', displayed: true, isStateChange: true)
}

def notready() { 
	log.info "Executing 'notready'"
    state.status = "notready"
	sendEvent(name: 'litterbox', value: 'notready', displayed: true, isStateChange: true)
}

def full() {
	log.info "Executing 'full'"
    state.status = "full"
	sendEvent(name: 'litterbox', value: 'full', displayed: true, isStateChange: true)
}

def count() {
	log.info "Increasing counter"
	if (state.count == null) { state.count = 0 }
	state.count = state.count + 1
	sendEvent(name: 'count', value: state.count, displayed: true, isStateChange: true)
}

def resetcount() {
	log.info "Resetting counter"
	state.count = 0
	sendEvent(name: 'count', value: state.count, displayed: true, isStateChange: true)
}