L = ["foo", "bar"]
a = '|'.join([r'\b%s\b' % word for word in L])
print(a)