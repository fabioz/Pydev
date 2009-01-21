# before
comprelist = [zahl ** 2 for zahl in range(9)] # on-line
# after
compre2list = (x ** 2 for x in range(5)) # on-line compre2
# after compre 2
compre3list = [zahl ** 2 for zahl in range(9) if zahl % 2 == 0] # on-line compre3
# after compre3
compre4list = [zahl ** 2 for zahl in (1, 4, 6) if zahl % 2 == 1 if zahl % 3 == 2] # on-line
print compre3list

