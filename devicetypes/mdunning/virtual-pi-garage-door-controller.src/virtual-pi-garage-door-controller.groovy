/**
 * Pi Garage Door Control
 *  
 * Copyright 2018 Matt Dunning
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
	definition (name: "Virtual Pi Garage Door Controller", namespace: "mdunning", author: "mdunning") {
		capability "Momentary"
        capability "Sensor"
		capability "Refresh"
		capability "Polling"
       
        attribute "door", "enum", ["closed", "closing", "stopped_closing", "open", "opening", "stopped_opening", "unknown"]
        attribute "button", "enum", ["default", "pushed"]
        
        command "changeSwitchState", ["string"]
        command "transitionSwitchState", []
        
	}
	
	simulator {
		// DO NOTHING
	}
	
	tiles {
		standardTile("door", "device.door", width: 2, height: 2) {
			state("closed", label:"Closed", action:"momentary.push", icon:"st.doors.garage.garage-closed", backgroundColor:"#06f906", nextState:"opening")
            state("closing", label:"Closing", action:"momentary.push", icon:"st.doors.garage.garage-closing", backgroundColor:"#f2f20d", nextState:"stopped_closing")
            state("stopped_closing", label:"Stopped", action:"momentary.push", icon:"st.doors.garage.garage-closing", backgroundColor:"#f90606", nextState:"opening")
			state("open", label:"Open", action:"momentary.push", icon:"st.doors.garage.garage-open", backgroundColor:"#06f906", nextState:"closing")
			state("opening", label:"Opening", action:"momentary.push", icon:"st.doors.garage.garage-opening", backgroundColor:"#f2f20d", nextState:"stopped_opening")
            state("stopped_opening", label:"Stopped", action:"momentary.push", icon:"st.doors.garage.garage-opening", backgroundColor:"#f90606", nextState: "closing")
            state("unknown", label:"Unknown", action:"momentary.push", icon:"st.doors.garage.garage-open", backgroundColor:"#0606f9")
		}
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
        	state "default", label:'refresh', action:"polling.poll", icon:"st.secondary.refresh-icon"
        }

		main "door"
		details(["door", "refresh"])
	}
    
}

// parse events into attributes
def parse(String description) {
	log.debug "Virtual siwtch parsing '${description}'"
}

def poll() {
	log.debug "Executing 'poll'"   
    
    sendEvent(name: "button", value: device.deviceNetworkId + ".pushed")
    sendEvent(name: "button", value: "pushed", data: [buttonPushed: "refresh"])
}

def push() {
	log.debug "Executing 'push'"	     
    log.debug device.currentValue("door")    
    
    transitionSwitchState();
    sendEvent(name: "button", value: device.deviceNetworkId + ".pushed")
    sendEvent(name: "button", value: "pushed", data: [buttonPushed: "doorOpener"])
}

def transitionSwitchState() {
	log.debug "Switch has been pushed, updating state"
    
    def newSwitchState = ""
    switch(device.currentValue("door")) {
    	case "closed":
        	newSwitchState = "opening"
            break
        case "closing":
        	newSwitchState = "stopped_closing"
            break
        case "stopped_closing":
        	newSwitchState = "opening"
            break
        case "open":
        	newSwitchState = "closing"
            break
        case "opening":
        	newSwitchState = "stopped_opening"
            break
        case "stopped_opening":
        	newSwitchState = "closing"
            break
    }
    
    if (!newSwitchState.equals("")) {
    	sendEvent(name: "door", value: newSwitchState)
    }
}

def changeSwitchState(newState) {
	log.debug "Received update from RPi vlaue is $newState"
    
	switch(newState) {
        case 1:
        	sendEvent(name: "door", value: "closed")
            break
    	case 0:
        	sendEvent(name: "door", value: "open")
            break
        case -1:
        	def currentSwitchState = device.currentValue("door")
            if (currentSwitchState.equals("closed")) {
            	sendEvent(name: "door", value: "opening")
            } else if (currentSwitchState.equals("open")) {
            	sendEvent(name: "door", value:"closing")
            } else {
        		sendEvent(name: "door", value: "unknown")
                sendNotification("Failed to determine if the garage door is opened or closed", [method: "push"])
            }
            break;
    }
}