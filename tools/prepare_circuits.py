"""Project and orient licensed GeoJSON into small offline circuit diagrams (no telemetry)."""
import json,math,sys,urllib.request
from pathlib import Path
root=Path(__file__).resolve().parents[1]
source=json.loads(Path(sys.argv[1]).read_text() if len(sys.argv)>1 else urllib.request.urlopen('https://raw.githubusercontent.com/bacinger/f1-circuits/394d8fbe70ef2c0b0c8d23ff7bee61fa09606055/f1-circuits.geojson',timeout=30).read())
ids={'albert_park':'au-1953','bahrain':'bh-2002','jeddah':'sa-2021','americas':'us-2012','miami':'us-2022','monaco':'mc-1929','baku':'az-2016','catalunya':'es-1991','villeneuve':'ca-1978','red_bull_ring':'at-1969','silverstone':'gb-1948','hungaroring':'hu-1986','spa':'be-1925','zandvoort':'nl-1948','monza':'it-1922','marina_bay':'sg-2008','suzuka':'jp-1962','losail':'qa-2004','rodriguez':'mx-1962','interlagos':'br-1940','vegas':'us-2023','yas_marina':'ae-2009','shanghai':'cn-2004','imola':'it-1953','madring':'es-2026','istanbul':'tr-2005','hockenheimring':'de-1932','nurburgring':'de-1927','ricard':'fr-1969','mugello':'it-1914','portimao':'pt-2008','sepang':'my-1999','sochi':'ru-2014'}
features={f['properties']['id']:f for f in source['features']}
result={}
for id,key in ids.items():
    f=features[key]; coords=f['geometry']['coordinates']; assert f['geometry']['type']=='LineString'
    lon=sum(p[0] for p in coords)/len(coords);lat=sum(p[1] for p in coords)/len(coords)
    coslat=math.cos(math.radians(lat)); pts=[((p[0]-lon)*coslat,lat-p[1]) for p in coords]
    xx=sum(x*x for x,y in pts);yy=sum(y*y for x,y in pts);xy=sum(x*y for x,y in pts)
    angle=.5*math.atan2(2*xy,xx-yy);ca=math.cos(angle);sa=math.sin(angle)
    result[id]=[[round(x*ca+y*sa,7),round(-x*sa+y*ca,7)] for x,y in pts]
output={'source':'https://github.com/bacinger/f1-circuits','commit':'394d8fbe70ef2c0b0c8d23ff7bee61fa09606055','license':'MIT / Tomislav Bacinger','treatment':'Local projection and principal-axis rotation for illustrative diagrams. Not official circuit surveys or live telemetry.','circuits':result}
(root/'app/src/main/assets/circuits/outlines.json').write_text(json.dumps(output,separators=(',',':')))
print(len(result),'licensed offline circuit outlines generated.')
if len(sys.argv)>2:
    calendar=json.loads(Path(sys.argv[2]).read_text())['MRData']['RaceTable']['Races']
    missing=[r['Circuit']['circuitId'] for r in calendar if r['Circuit']['circuitId'] not in result]
    print(len(calendar)-len(missing),'/',len(calendar),'published circuits matched; missing:',missing)
