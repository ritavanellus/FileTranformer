# FileTransformer
Java code to transform kml files into a JSON used for a GoogleMaps project (see Repository "LandOfTheGuardians")
</br></br>
The website "Land of the Guardians" (see repo with same name) needs information to show length and elevation profile of a chosen route and display it on a GoogleMaps map. This information is read in as a JSON file, where route sections are represented as JavaScript objects. 
</br>
Original coordinates and altitude data of the route sections are downloaded from GoogleMaps and GPS Visualizer (the latter is used to add altitude data) in .kml format. The present Java code reads these files in, subtracts and generates data, then writes it into a JSON file. The code needs the following inputs: <ul>
<li> a .txt file containing the list of all file names that needs to be read in </li>
<li> a .txt file containing the full name of all node points
<li> .kml files containing coordinates and altitude data </li>
<li> name of the output JSON file </li>
<li> path URLs of the above mentioned items </li>
</ul>
</br>
The .kml files are named following a special logic. An example is "SGC_MUU_01_A_02x23". This file name represents route nr 1 (in case alternative routes exist between these two node points) between node points "SGC" and "MUU", which is an asphalt road of a length 2,23 km. 




