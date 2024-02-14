#!/usr/bin/env python3
import sys
f=open('Report_Saeedeh/t.txt')
lines=f.readlines()
f.close()


parameters=[


	#'cloud Total Idle Networking Energy with Flow-base model', 
	#'cloud Total Active Networking Energy with Flow-base model',
	#'cloud Total Device Networking Energy with Flow-base model',

	#'proxy-server Total Idle Networking Energy with Flow-base model',
	#'proxy-server Total Active Networking Energy with Flow-base model',
	#'proxy-server Total Device Networking Energy with Flow-base model',

	#'d-0 Total Idle Networking Energy with Flow-base model',
	#'d-0 Total Active Networking Energy with Flow-base model',
	#'d-0 Total Device Networking Energy with Flow-base model',

	#'m-0-0 Total Idle Networking Energy with Flow-base model',
	#'m-0-0 Total Active Networking Energy with Flow-base model',
	#'m-0-0 Total Device Networking Energy with Flow-base model',

	'cloud : Energy Consumed', 
	'cloud : Application Energy Consumed',
	'proxy-server : Energy Consumed',
	'proxy-server : Application Energy Consumed',
	'd-0 : Energy Consumed',
	'd-0 : Application Energy Consumed',
	'm-0-0 : Energy Consumed',
	'm-0-0 : Application Energy Consumed',
	'EXECUTION TIME',
	'[motion_detector, object_detector, object_tracker] --->',
	'[object_tracker, PTZ_CONTROL] --->',
	'MOTION_VIDEO_STREAM',
	'DETECTED_OBJECT',
	'OBJECT_LOCATION',
	'CAMERA --->',
	'Cost of execution in cloud',
	'Total network usage',
	#'Total network Energy',
	#'Total networking Time',
	#'cloud : South Link Total Energy',
	#'cloud : Active Energy of South Link',
	#'proxy-server : North Link Total Energy',
	#'proxy-server : Active Energy of North Link',
	#'proxy-server : South Link Total Energy',
	#'proxy-server : Active Energy of South Link',
	#'d-0 : North Link Total Energy',
	#'d-0 : Active nergy of North Link',
	#'d-0 : South Link Total Energy',
	#'d-0 : Active Energy of South Link',
	#'m-0-0 : North Link Total Energy',
	#'m-0-0 : Active Energy of North Link'
	
	'cloud : Application dcns_1 total Energy',
	'cloud : Application dcns_0 total Energy',
	
	
	
	'cloud : NIC Device Total Energy',
	'cloud : NIC APP Active Energy',
	'proxy-server : NIC Device Total Energy',
	'proxy-server : NIC App Active Energy',
	'd-0 : NIC Device Total Energy',
	'd-0 : NIC App Active Energy',
	'm-0-0 : NIC Device Total Energy',
	'm-0-0 : NIC App Active Energy',
	
	'cloud : measured_NIC_Device_Total_Energy',
	'cloud : measured_NIC_App_Active_Energy',
	'proxy-server : measured_NIC_Device_Total_Energy',
	'proxy-server : measured_NIC_App_Active_Energy',
	'd-0 : measured_NIC_Device_Total_Energy',
	'd-0 : measured_NIC_App_Active_Energy',
	'm-0-0 : measured_NIC_Device_Total_Energy',
	'm-0-0 : measured_NIC_App_Active_Energy'
	]
	#'Network usage-based energy',
	#'Uplink energy of device Id : 12',
	#'Uplink energy of device Id : 15',
	#'Uplink energy of device Id : 9',
	#'Uplink energy of device Id : 6',
	#'Downlink energy of device Id : 12',
	#'Downlink energy of device Id : 15',
	#'Downlink energy of device Id : 9',
	#'Downlink energy of device Id : 6',
	#'Total link energy of device Id : 12',
	#'Total link energy of device Id : 15',
	#'Total link energy of device Id : 9',
	#'Total link energy of device Id : 6']
	#'Uplink energy of device Id : 4',
	#'Downlink energy of device Id : 4',
	#'Total link energy of device Id : 4',
	#'Uplink energy of device Id : 5',
	#'Downlink energy of device Id : 5',
	#'Total link energy of device Id : 5']
	
mapping={
	#'cloud Total Idle Networking Energy with Flow-base model': 'cloud Flow-based Total Idle Networking Energy', 
	#'cloud Total Active Networking Energy with Flow-base model':'cloud Flow-based Total Active Networking Energy',
	#'cloud Total Device Networking Energy with Flow-base model':'cloud Flow-based Total Device Networking Energy',

	#'proxy-server Total Idle Networking Energy with Flow-base model':'proxy-server Flow-based Total Idle Networking Energy',
	#'proxy-server Total Active Networking Energy with Flow-base model':'proxy-server Flow-based Total Active Networking Energy',
	#'proxy-server Total Device Networking Energy with Flow-base model':'proxy-server Flow-based Total Device Networking Energy',

	#'d-0 Total Idle Networking Energy with Flow-base model':'d-0 Flow-based Total Idle Networking Energy',
	#'d-0 Total Active Networking Energy with Flow-base model':'d-0 Flow-based Total Active Networking Energy',
	#'d-0 Total Device Networking Energy with Flow-base model':'d-0 Flow-based Total Device Networking Energy',

	#'m-0-0 Total Idle Networking Energy with Flow-base model':'m-0-0 Flow-based Total Idle Networking Energy',
	#'m-0-0 Total Active Networking Energy with Flow-base model':'m-0-0 Flow-based Total Active Networking Energy',
	#'m-0-0 Total Device Networking Energy with Flow-base model':'m-0-0 Flow-based Total Device Networking Energy',


	'cloud : Energy Consumed':'Cloud Energy',
	'cloud : Application Energy Consumed':'App_Cloud Energy',
	'proxy-server : Energy Consumed':'Proxy Energy',
	'proxy-server : Application Energy Consumed':'App_Proxy Energy',
	'd-0 : Energy Consumed':'Router Energy',
	'd-0 : Application Energy Consumed':'App_Router Energy',
	'm-0-0 : Energy Consumed':'Mobile Energy',
	'm-0-0 : Application Energy Consumed':'App_Mobile Energy',
	'EXECUTION TIME':'Execution time',
	'[motion_detector, object_detector, object_tracker] --->':'Delay_1',
	'[object_tracker, PTZ_CONTROL] --->':'Delay_2',
	'MOTION_VIDEO_STREAM':'MOTION_VIDEO_STREAM',
	'DETECTED_OBJECT':'DETECTED_OBJECT',
	'OBJECT_LOCATION':'OBJECT_LOCATION',
	'CAMERA --->':'CAMERA',
	'Cost of execution in cloud':'Cost of execution in cloud',
	'Total network usage':'Total network usage',
	'Total network Energy':'Total network Energy',
	'Total networking Time':'Total networking Time',
	'cloud : South Link Total Energy':'cloud South Link Total Energy',
	'cloud : Active Energy of South Link':'cloud Active Energy of South Link',
	'proxy-server : North Link Total Energy':'Proxy North Link Total Energy',
	'proxy-server : Active Energy of North Link':'Proxy Active Energy of North Link',
	'proxy-server : South Link Total Energy':'Proxy South Link Total Energy',
	'proxy-server : Active Energy of South Link':'Proxy Active Energy of South Link',
	'd-0 : North Link Total Energy':'Router North Link Total Energy',
	'd-0 : Active nergy of North Link':'Router Active nergy of North Link',
	'd-0 : South Link Total Energy':'Router South Link Total Energy',
	'd-0 : Active Energy of South Link':'Router Active Energy of South Link',
	'm-0-0 : North Link Total Energy':'Mobile North Link Total Energy',
	'm-0-0 : Active Energy of North Link':'Mobile Active Energy of North Link',
	
	'cloud : Application dcns_1 total Energy' :'cloud : Application dcns_1 total Energy',
	'cloud : Application dcns_0 total Energy' :'cloud : Application dcns_0 total Energy';
	
	'cloud : NIC Device Total Energy' : 'Rated_Cloud NIC Total energy',
	'cloud : NIC APP Active Energy' : 'Rated_Cloud NIC Application energy',
	'proxy-server : NIC Device Total Energy' : 'Rated_proxy-server NIC Total energy',
	'proxy-server : NIC App Active Energy': 'Rated_proxy-server NIC Application energy',
	'd-0 : NIC Device Total Energy':'Rated_Router NIC Total energy',
	'd-0 : NIC App Active Energy':'Rated_Router NIC Application energy',
	'm-0-0 : NIC Device Total Energy':'Rated_Mobile Device NIC Total energy',
	'm-0-0 : NIC App Active Energy':'Rated_Mobile Device NIC Application energy',
	
	'cloud : measured_NIC_Device_Total_Energy':'Measured_Cloud NIC Total energy',
	'cloud : measured_NIC_App_Active_Energy':'Measured_Cloud NIC Application energy',
	'proxy-server : measured_NIC_Device_Total_Energy':'Measured_proxy-server NIC Total energy',
	'proxy-server : measured_NIC_App_Active_Energy':'Measured_proxy-server NIC Application energy',
	'd-0 : measured_NIC_Device_Total_Energy':'Measured_Router NIC Total energy',
	'd-0 : measured_NIC_App_Active_Energy':'Measured_Router NIC Application energy',
	'm-0-0 : measured_NIC_Device_Total_Energy':'Measured_Mobile Device NIC Total energy',
	'm-0-0 : measured_NIC_App_Active_Energy':'Measured_Mobile Device NIC Application energy'
	
	}
	#'Network usage-based energy':'Network usage-based energy',
	#'Uplink energy of device Id : 12':'UpLink energy of mobile device 12',
	#'Uplink energy of device Id : 15':'UpLink energy of mobile device 15',
	#'Uplink energy of device Id : 9':'UpLink energy of mobile device 9',
	#'Uplink energy of device Id : 6':'UpLink energy of mobile device 6',
	#'Downlink energy of device Id : 12':'DownLink energy of mobile device 12',
	#'Downlink energy of device Id : 15':'DownLink energy of mobile device 15',
	#'Downlink energy of device Id : 9':'DownLink energy of mobile device 9',
	#'Downlink energy of device Id : 6':'DownLink energy of mobile device 6',
	#'Total link energy of device Id : 12':'Total link energy of device Id : 12',
	#'Total link energy of device Id : 15':'Total link energy of device Id : 15',
	#'Total link energy of device Id : 9':'Total link energy of device Id : 9',
	#'Total link energy of device Id : 6':'Total link energy of device Id : 6'}
	#'Uplink energy of device Id : 4':'Uplink energy of Proxy 4',
	#'Downlink energy of device Id : 4':'Downlink energy of Proxy 4',
	#'Total link energy of device Id : 4':'Total link energy of Proxy 4',
	#'Uplink energy of device Id : 5':'Uplink energy of Router 5',
	#'Downlink energy of device Id : 5':'Downlink energy of Router 5',
	#'Total link energy of device Id : 5':'Total link energy of Router 5'}


results={}
for p in parameters:
	results[mapping[p]]=[]
	
for line in lines:
	for p in parameters:
		#if p in line:
		if line.startswith(p):
			value=float(line.split('=')[-1].strip().replace("}",""))
			results[mapping[p]].append(value)
			#print(f'appending value {value} for key {mapping[p]}')
			

for key in results:
	#print(key)
	results[key].append(min(results[key]))
	results[key].append(max(results[key]))
	results[key].append(sum(results[key])/len(results[key]))
			


#print(results)

resFileName=sys.argv[1]
res=open('Report_Saeedeh/'+resFileName+'.csv','w')

# writing header (#run):
N=len(results[list(results.keys())[0]])-3
#N=sys.argv[2]

for i in range(N):
	res.write(','+str(i+1))
res.write(',Min,Max,Avg\n')

# Writing results in excel file
for key in results:
	res.write(key+',')
	for value in results[key]:
		res.write(str(value)+',')
		
	res.write('\n')
	
res.close()	
		



