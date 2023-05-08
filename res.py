#!/usr/bin/env python3
import sys
f=open('Report_Saeedeh/t.txt')
lines=f.readlines()
f.close()


parameters=['cloud : Energy Consumed', 
	'cloud : Application Energy Consumed',
	'proxy-server : Energy Consumed',
	'proxy-server : Application Energy Consumed',
	'd-0 : Energy Consumed',
	'd-0 : Application Energy Consumed',
	'm-0-0 : Energy Consumed',
	'm-0-0 : Application Energy Consumed',
	'EXECUTION TIME',
	'[motion_detector, object_detector, object_tracker]',
	'[object_tracker, PTZ_CONTROL]',
	'MOTION_VIDEO_STREAM',
	'DETECTED_OBJECT',
	'OBJECT_LOCATION',
	'CAMERA --->',
	'Cost of execution in cloud',
	'Total network usage',
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
	
mapping={'cloud : Energy Consumed':'Cloud Energy',
	'cloud : Application Energy Consumed':'App_Cloud Energy',
	'proxy-server : Energy Consumed':'Proxy Energy',
	'proxy-server : Application Energy Consumed':'App_Proxy Energy',
	'd-0 : Energy Consumed':'Router Energy',
	'd-0 : Application Energy Consumed':'App_Router Energy',
	'm-0-0 : Energy Consumed':'Mobile Energy',
	'm-0-0 : Application Energy Consumed':'App_Mobile Energy',
	'EXECUTION TIME':'Execution time',
	'[motion_detector, object_detector, object_tracker]':'Delay_1',
	'[object_tracker, PTZ_CONTROL]':'Delay_2',
	'MOTION_VIDEO_STREAM':'MOTION_VIDEO_STREAM',
	'DETECTED_OBJECT':'DETECTED_OBJECT',
	'OBJECT_LOCATION':'OBJECT_LOCATION',
	'CAMERA --->':'CAMERA',
	'Cost of execution in cloud':'Cost of execution in cloud',
	'Total network usage':'Total network usage',
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
		if p in line:
			value=float(line.split('=')[-1].strip())
			results[mapping[p]].append(value)
			

for key in results:
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
		



