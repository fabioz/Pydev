
# DO NOT MODIFY - THIS WORKS (PYDEV SHOWS A FAILURE)
with open('testWith.py', 'r') as f: # on-line
    # after
    for line in f:
        print(line)

with True: # on-line
    # just afer whith true
    if (False == True):
        print("with is dangerous :p")
    