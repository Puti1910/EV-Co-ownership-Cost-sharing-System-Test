import zipfile
import os
import shutil

jar_path = 'app-temp.jar'
new_jar_path = 'app-temp-new.jar'
file_to_replace = 'BOOT-INF/classes/static/js/fairness/user-fair-schedule.js'
local_file = r'ui-service\src\main\resources\static\js\fairness\user-fair-schedule.js'

try:
    with open(local_file, 'rb') as f:
        new_data = f.read()

    zin = zipfile.ZipFile(jar_path, 'r')
    zout = zipfile.ZipFile(new_jar_path, 'w', zipfile.ZIP_DEFLATED)
    
    replaced = False
    for item in zin.infolist():
        if item.filename == file_to_replace:
            zout.writestr(item, new_data)
            replaced = True
        else:
            buffer = zin.read(item.filename)
            zout.writestr(item, buffer)
            
    zin.close()
    zout.close()
    
    if replaced:
        print("Successfully updated user-fair-schedule.js inside the JAR.")
    else:
        print("Error: Could not find {} in the JAR.".format(file_to_replace))

except Exception as e:
    print("An error occurred: {}".format(e))
