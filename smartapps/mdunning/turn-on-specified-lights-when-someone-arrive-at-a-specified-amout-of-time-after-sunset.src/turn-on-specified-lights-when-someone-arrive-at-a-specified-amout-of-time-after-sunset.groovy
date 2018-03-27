/**
 * Turn on only after arriving a specified about of time before or after sunset
 *
 *  Author: Matt Dunning
 */
definition(
    name: "Turn On Specified Lights When Someone Arrive At A Specified Amout Of Time After Sunset",
    namespace: "mdunning",
    author: "Matt Dunning",
    description: "Turn something on only if you arrive a specified amout of time before or after sunset.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_presence-outlet@2x.png"
)

preferences {
    section ("What do you want to happen?") {
    	input (name: "switches", 
               type: "capability.switch", 
               title: "Turn On These Switches", 
               multiple: true)
    
        input (name: "newMode", 
               type: "mode", 
               title: "Change the mode to",
               required: true)
	}
    
    section ("Additional Settings:") {
    	input (name: "brightness", 
        	   type: "enum", 
               title: "Set Dimmers To This Level", 
               options: [["10":"10%"], ["20":"20%"], ["30":"30%"], ["40":"40%"], ["50":"50%"], ["60":"60%"], ["70":"70%"], ["80":"80%"], ["90":"90%"], ["100":"100%"]])
               
        input (name: "beforeAfter", 
               type: "enum", 
               title: "Before or After", 
               options: ["", "Before", "After"], 
               required: true)
               
        input (name: "minutes", 
               type: "number", 
               title: "Time Before or After Sunset (In Minutes)",
               range: "0..1440")
    }
}

def installed() {
	subscribe(location, "mode", modeHandler)
}

def updated() {
	unsubscribe()
	subscribe(location, "mode", modeHandler)
}

/**
 * Handler method for mode changes
 *
 * @evt the event data associated with the handler
 **/
def modeHandler(evt) {
	log.info "Checking to see if lights should be turned on"
	def now = new Date()
    def currentMode = evt.value
    def triggerTime = getTriggerTime(beforeAfter, minutes) 
    
   if(location.currentMode == "Arriving" && (now > triggerTime)) {
       	switches.each{
            if (it.latestValue("level").toString() != "null" ) {
            	it.setLevel(brightness)
            }
            it.on()
        }   
	}
    
    if (location.mode != newMode) {
        if (location.modes?.find{it.name == newMode}) {
            setLocationMode(newMode)
        } else {
             log.warn "Tried to change to undefined mode '$newMode'"
        }
    }
}

/**
 * Calculate the trigger time based on the passed in parameters
 * 
 * @param beforeAfter - enum value to indicate that the trigger time is before or after sunset
 * @param delay - how much time, in minutes, to add or subtract from sunset
 * @return the trigger time to check against
 **/
def Date getTriggerTime (beforeAfter, delay) {
	def triggerTime
    def StringBuilder offSet = new StringBuilder()
    int hours = delay / 60
    int minutes = delay % 60
    
    if (beforeAfter == 1) {
    	offSet.append("-")
    } 
    offSet.append(hours)
    offSet.append(":")
    offSet.append(minutes)
    
    triggerTime = getSunriseAndSunset(sunsetOffset: offSet.toString())
    
    return triggerTime.sunset
}