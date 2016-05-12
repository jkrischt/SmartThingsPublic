/**
 *  Copyright 2015 SmartThings
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
    definition (name: "WorksWith Dimmer", namespace: "jkrischt", author: "Brad Krischke") {
        capability "Actuator"
        capability "Button"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
        capability "Switch Level"

        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0B04"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0702"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0702, 0B05", outClusters: "0019", manufacturer: "sengled", model: "Z01-CIA19NAE26", deviceJoinName: "Sengled Element touch"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0702, 0B05", outClusters: "000A, 0019", manufacturer: "Jasco Products", model: "45852", deviceJoinName: "GE Zigbee Plug-In Dimmer"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0702, 0B05", outClusters: "000A, 0019", manufacturer: "Jasco Products", model: "45857", deviceJoinName: "GE Zigbee In-Wall Dimmer"
    }

    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        main "switch"
        details(["switch", "refresh"])
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
    Map map = [:]
    log.debug "description is $description"
	
    def event = zigbee.getEvent(description)
    
    if (description?.startsWith('catchall:')) {
       // call parseCatchAllMessage to parse the catchall message received
       map = parseCatchAllMessage(description)
       if (map.value == "pushed") {
         List cmds = onoffResponse()
         def result = []
    	 log.trace "Sending current state to device ${cmds}"
         result = cmds?.collect { new physicalgraph.device.HubAction(it) }  
         return result
         //createEvent(map)
         //return map
       }
    }
    
    if (event) {
    	log.info event
    	if (event.name == "level") {
    		event.value = (event.value as Integer) * 100 / 39  //update level to overcome return of 39 as max from switch
            sendEvent(event)
    	}
    	else {
       		sendEvent(event)
        }
    }
    else {
        log.warn "DID NOT PARSE MESSAGE for description : $description"
        log.debug zigbee.parseDescriptionAsMap(description)
    }
}

def onoffResponse() {
	log.debug "Creating on/off response"
    def swtch = device.currentState("switch")?.value												// Get the current on off value
    if (swtch == "on"){
        swtch = "0"								
    }else{
        swtch = "1"							
    }    
    [
       	"st cmd 0x${device.deviceNetworkId} 1 6 ${swtch} {}"										// Set On or Off
    ]
}

def off() {
    zigbee.off()
}

def on() {
    zigbee.on()
}

def setLevel(value) {
    zigbee.setLevel(value*39/100)
}

def refresh() {
    zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.simpleMeteringPowerRefresh() + zigbee.electricMeasurementPowerRefresh() + zigbee.onOffConfig() + zigbee.levelConfig() + zigbee.simpleMeteringPowerConfig() + zigbee.electricMeasurementPowerConfig()
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    zigbee.onOffConfig() + zigbee.levelConfig() + zigbee.simpleMeteringPowerConfig() + zigbee.electricMeasurementPowerConfig() + zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.simpleMeteringPowerRefresh() + zigbee.electricMeasurementPowerRefresh()
}

private Map parseCatchAllMessage(String description) {
  // Create a map from the raw zigbee message to make parsing more intuitive
  def msg = zigbee.parse(description)
  Map result = [:]
  switch(msg.clusterId) {
    case 6:
      switch(msg.command) {
        case 2: // button pressed
          result = [
            name: 'button',
            value: 'pushed',
            data: [buttonNumber: 1],
            descriptionText: "$device.displayName button was pressed",
            isStateChange: true
          ]
          log.debug "Parse returned ${result?.descriptionText}"
          return result
          break
      }
  }
  result = [value: 'nothing']
}