/**
*  OSRAM Lightify Dimming Switch
*
*  Copyright 2016 Michael Hudson
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
*  Thanks to @Sticks18 for the Hue Dimmer remote code used as a base for this!
*
*/

metadata {
  definition (name: "OSRAM Lightify Dimming Switch Test", namespace: "jkrischt", author: "Michael Hudson") {
    capability "Actuator"
    capability "Battery"
    capability "Button"
    capability "Configuration"
    capability "Refresh"
    capability "Sensor"
       
    attribute "zMessage", "String"
       
    //fingerprint profileId: "0104", deviceId: "0001", inClusters: "0000, 0001, 0003, 0020, 0402, 0B05", outClusters: "0003, 0006, 0008, 0019", /*manufacturer: "OSRAM", model: "Lightify 2.4GHZZB/SWITCH/LFY", */deviceJoinName: "OSRAM Lightify Dimming Switch"
  }

  // simulator metadata
  simulator {
    // status messages
 
  }
  
  
    preferences {
        input("device1", "string", title:"Device Network ID 1", description: "Device Network ID 1", defaultValue: "" ,required: false, displayDuringSetup: false)
        input("end1", "string", title:"Device Endpoint ID 1", description: "Device Endpoint ID 1", defaultValue: "" ,required: false, displayDuringSetup: false)
        input("device2", "string", title:"Device Network ID 2", description: "Device Network ID 2", defaultValue: "" ,required: false, displayDuringSetup: false)
        input("end2", "string", title:"Device Endpoint ID 2", description: "Device Endpoint ID 2", defaultValue: "" ,required: false, displayDuringSetup: false)
        input("device3", "string", title:"Device Network ID 3", description: "Device Network ID 3", defaultValue: "" ,required: false, displayDuringSetup: false)
        input("end3", "string", title:"Device Endpoint ID 3", description: "Device Endpoint ID 3", defaultValue: "" ,required: false, displayDuringSetup: false)
        input("device4", "string", title:"Device Network ID 4", description: "Device Network ID 4", defaultValue: "" ,required: false, displayDuringSetup: false)
        input("end4", "string", title:"Device Endpoint ID 4", description: "Device Endpoint ID 4", defaultValue: "" ,required: false, displayDuringSetup: false)
        input("device5", "string", title:"Device Network ID 5", description: "Device Network ID 5", defaultValue: "" ,required: false, displayDuringSetup: false)
        input("end5", "string", title:"Device Endpoint ID 5", description: "Device Endpoint ID 5", defaultValue: "" ,required: false, displayDuringSetup: false)
        input("dim1", "number", title:"Dim Level Uppper", description: "Dim Level Upppr", defaultValue: "" , required: false, displayDuringSetup: false)
        input("dim2", "number", title:"Dim Level Lower", description: "Dim Level Lower", defaultValue: "" , required: false, displayDuringSetup: false)
	}


  // UI tile definitions
  tiles(scale: 2) {
    standardTile("button", "device.button", width: 6, height: 4) {
      state "default", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
    }
    valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
      state "battery", label:'${currentValue}% battery'
    }
    standardTile("refresh", "device.button", decoration: "flat", width: 2, height: 2) {
      state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    main "button"
    details(["button", "battery", "refresh"])
  }
}

// Parse incoming device messages to generate events
def parse(String description) {
  Map map = [:]
  def result = []
  log.debug "parse description: $description"
  if (description?.startsWith('catchall:')) {
    // call parseCatchAllMessage to parse the catchall message received
    map = parseCatchAllMessage(description)
    if (device1 == null) {} else {
		if (map.value == "pushed") {
    		if (map.data.buttonNumber == 1) {
        		List cmds = onResponse()
    			log.trace "Sending current state to device ${cmds}"
        		result = cmds?.collect { new physicalgraph.device.HubAction(it) }  
        		return result
    		} else {
        		List cmds = offResponse()
    			log.trace "Sending current state to device ${cmds}"
        		result = cmds?.collect { new physicalgraph.device.HubAction(it) }  
        		return result
        	}
    	}
    }
	if (device1 == null) {} else {
        if (map.value == "held") {
        	if (map.data.buttonNumber == 1) {
        		List cmds = dim1Response()
    			log.trace "Sending current state to device ${cmds}"
        		result = cmds?.collect { new physicalgraph.device.HubAction(it) }  
        		return result
    		} else {
        		List cmds = dim2Response()
    			log.trace "Sending current state to device ${cmds}"
        		result = cmds?.collect { new physicalgraph.device.HubAction(it) }  
        		return result
        	}
        }
    }
  } else if (description?.startsWith('read')) {
    // call parseReadMessage to parse the read message received
    map = parseReadMessage(description)
  } else {
    log.debug "Unknown message received: $description"
  }
  return map ? createEvent(map) : null
}

def configure() {
  log.debug "Confuguring Reporting and Bindings."
  def configCmds = [
    // Bind the outgoing on/off cluster from remote to hub, so the hub receives messages when On/Off buttons pushed
    "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0006 {${device.zigbeeId}} {}",

    // Bind the outgoing level cluster from remote to hub, so the hub receives messages when Dim Up/Down buttons pushed
    "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0008 {${device.zigbeeId}} {}",
    
    // Bind the incoming battery info cluster from remote to hub, so the hub receives battery updates
    "zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x0001 {${device.zigbeeId}} {}",
  ]
  return configCmds 
}

def refresh() {
  def refreshCmds = [
    zigbee.readAttribute(0x0001, 0x0020)
  ]
  //when refresh button is pushed, read updated status
  return refreshCmds
}

private Map parseReadMessage(String description) {
  // Create a map from the message description to make parsing more intuitive
  def msg = zigbee.parseDescriptionAsMap(description)
  //def msg = zigbee.parse(description)
  if (msg.clusterInt==1 && msg.attrInt==32) {
    // call getBatteryResult method to parse battery message into event map
    def result = getBatteryResult(Integer.parseInt(msg.value, 16))
  } else {
    log.debug "Unknown read message received, parsed message: $msg"
  }
  // return map used to create event
  return result
}

private Map parseCatchAllMessage(String description) {
  // Create a map from the raw zigbee message to make parsing more intuitive
  def msg = zigbee.parse(description)
  log.debug msg
  switch(msg.clusterId) {
    case 1:
      // call getBatteryResult method to parse battery message into event map
      log.debug 'BATTERY MESSAGE'
      def result = getBatteryResult(Integer.parseInt(msg.value, 16))
      break
    case 6:
      def button = (msg.command == 1 ? 1 : 2)
      Map result = [:]
      result = [
        name: 'button',
        value: 'pushed',
        data: [buttonNumber: button],
        descriptionText: "$device.displayName button $button was pushed",
        isStateChange: true
      ]
      log.debug "Parse returned ${result?.descriptionText}"
      return result
      break
    case 8:
      switch(msg.command) {
        case 1: // brightness decrease command
          Map result = [:]
          result = [
            name: 'button',
            value: 'held',
            data: [buttonNumber: 2],
            descriptionText: "$device.displayName button 2 was held",
            isStateChange: true
          ]
          log.debug "Parse returned ${result?.descriptionText}"
          return result
          break
        case 3: // brightness change stop command
          def result = [
            name: 'button',
            value: 'released'
            //data: [buttonNumber: [1,2]],
            //descriptionText: "$device.displayName button was released",
            //isStateChange: true
          ]
          log.debug "Recieved stop command, not currently implemented!"
          return result
          break
        case 5: // brightness increase command
          Map result = [:]
          result = [
            name: 'button',
            value: 'held',
            data: [buttonNumber: 1],
            descriptionText: "$device.displayName button 1 was held",
            isStateChange: true
          ]
          log.debug "Parse returned ${result?.descriptionText}"
          return result
          break
      }
  }
}

//obtained from other examples, converts battery message into event map
private Map getBatteryResult(rawValue) {
  def linkText = getLinkText(device)
  def result = [
    name: 'battery',
    value: '--'
  ]
  def volts = rawValue / 10
  def descriptionText
  if (rawValue == 0) {
  } else {
    if (volts > 3.5) {
      result.descriptionText = "${linkText} battery has too much power (${volts} volts)."
    } else if (volts > 0){
      def minVolts = 2.1
      def maxVolts = 3.0
      def pct = (volts - minVolts) / (maxVolts - minVolts)
      result.value = Math.min(100, (int) pct * 100)
      result.descriptionText = "${linkText} battery was ${result.value}%"
    }
  }
  log.debug "Parse returned ${result?.descriptionText}"
  return result
}

def onResponse() {
	log.debug "Creating on response"
    def on1 = "st cmd 0x${device1} 0x${end1} 8 4 {FE 0000}"
    def on2
    def on3
    def on4
    def on5
    if (device2 == null) {} else {
       	on2 = "st cmd 0x${device2} 0x${end2} 8 4 {FE 0000}"
	}
    if (device3 == null) {} else {
       	on3 = "st cmd 0x${device3} 0x${end3} 8 4 {FE 0000}"										
	}
    if (device4 == null) {} else {
       	on4 = "st cmd 0x${device4} 0x${end4} 8 4 {FE 0000}"
	}
    if (device5 == null) {} else {
       	on5 = "st cmd 0x${device5} 0x${end5} 8 4 {FE 0000}"
	}
    
    if (device5 == null) {
    if (device4 == null) {
    if (device3 == null) {
    if (device2 == null) {
    	[
        on1
        ]
        }
	else {
    	[
        on1,
        on2
        ] 
        }
	} else {
    	[
        on1,
        on2,
        on3
        ] 
        }
	} else {
    	[
        on1,
        on2,
        on3,
        on4
        ]
        }
    } else {
    	[
        on1,
        on2,
        on3,
        on4,
        on5
        ]
    	}
}

def offResponse() {
	log.debug "Creating off response"
    def off1 = "st cmd 0x${device1} 0x${end1} 6 0 {}"
    def off2
    def off3
    def off4
    def off5
    if (device2 == null) {} else {
       	off2 = "st cmd 0x${device2} 0x${end2} 6 0 {}"
	}
    if (device3 == null) {} else {
       	off3 = "st cmd 0x${device3} 0x${end3} 6 0 {}"										
	}
    if (device4 == null) {} else {
       	off4 = "st cmd 0x${device4} 0x${end4} 6 0 {}"
	}
    if (device5 == null) {} else {
       	off5 = "st cmd 0x${device5} 0x${end5} 6 0 {}"
	}
    
    if (device5 == null) {
    if (device4 == null) {
    if (device3 == null) {
    if (device2 == null) {
    	[
        off1
        ]
        }
	else {
    	[
        off1,
        off2
        ] 
        }
	} else {
    	[
        off1,
        off2,
        off3
        ] 
        }
	} else {
    	[
        off1,
        off2,
        off3,
        off4
        ]
        }
    } else {
    	[
        off1,
        off2,
        off3,
        off4,
        off5
        ]
    	}
}

def dim1Response() {
	log.debug "Creating dim1 response"
    def dim1 = hexString(Math.round(dim1 * 255/100))
    def d1 = "st cmd 0x${device1} 0x${end1} 8 4 {${dim1} 0000}"
    def d2
    def d3
    def d4
    def d5
    if (device2 == null) {} else {
       	d2 = "st cmd 0x${device2} 0x${end2} 8 4 {${dim1} 0000}"
	}
    if (device3 == null) {} else {
       	d3 = "st cmd 0x${device3} 0x${end3} 8 4 {${dim1} 0000}"										
	}
    if (device4 == null) {} else {
       	d4 = "st cmd 0x${device4} 0x${end4} 8 4 {${dim1} 0000}"
	}
    if (device5 == null) {} else {
       	d5 = "st cmd 0x${device5} 0x${end5} 8 4 {${dim1} 0000}"
	}
    
    if (device5 == null) {
    if (device4 == null) {
    if (device3 == null) {
    if (device2 == null) {
    	[
        d1
        ]
        }
	else {
    	[
        d1,
        d2
        ] 
        }
	} else {
    	[
        d1,
        d2,
        d3
        ] 
        }
	} else {
    	[
        d1,
        d2,
        d3,
        d4
        ]
        }
    } else {
    	[
        d1,
        d2,
        d3,
        d4,
        d5
        ]
    	}
}

def dim2Response() {
	log.debug "Creating dim2 response"
    def dim2 = hexString(Math.round(dim2 * 255/100))
    def d1 = "st cmd 0x${device1} 0x${end1} 8 4 {${dim2} 0000}"
    def d2
    def d3
    def d4
    def d5
    if (device2 == null) {} else {
       	d2 = "st cmd 0x${device2} 0x${end2} 8 4 {${dim2} 0000}"
	}
    if (device3 == null) {} else {
       	d3 = "st cmd 0x${device3} 0x${end3} 8 4 {${dim2} 0000}"										
	}
    if (device4 == null) {} else {
       	d4 = "st cmd 0x${device4} 0x${end4} 8 4 {${dim2} 0000}"
	}
    if (device5 == null) {} else {
       	d5 = "st cmd 0x${device5} 0x${end5} 8 4 {${dim2} 0000}"
	}
    
    if (device5 == null) {
    if (device4 == null) {
    if (device3 == null) {
    if (device2 == null) {
    	[
        d1
        ]
        }
	else {
    	[
        d1,
        d2
        ] 
        }
	} else {
    	[
        d1,
        d2,
        d3
        ] 
        }
	} else {
    	[
        d1,
        d2,
        d3,
        d4
        ]
        }
    } else {
    	[
        d1,
        d2,
        d3,
        d4,
        d5
        ]
    	}
}
