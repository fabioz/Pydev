def fib(n): # Gib Fibonacci-Reihe bis n aus.
    a, b = 0, 1
    while b < n:
        print(b, end=' ')
        a, b = b, a+b

def fib2(n): # Gib Fibonacci-Reihe bis n aus.
    result = []
    a, b = 0, 1
    while b < n:
        result.append(b)
        a, b = b, a+b
    return result