package natalia.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MyTest {
    @Test
    public void somaDoisValores() throws Exception {
        int valorA = 1;
        int valorB = 2;
        Calculadora calculadora = new Calculadora();
        int soma = calculadora.soma(valorA, valorB);

        assertEquals(3, soma);
    }

    @Test
    public void diminuiDoisValores() throws Exception {
        Calculadora calculadora = new Calculadora();
        int valorA = 1;
        int valorB = 2;
        int soma = calculadora.subtrai(valorA, valorB);

        assertEquals(-1, soma);
    }

    @Test
    public void divideDoisValores() throws Exception {
        int valorA = 6;
        int valorB = 3;
        Calculadora calculadora = new Calculadora();
        int divisao = calculadora.divide(valorA, valorB);

        assertEquals(2, divisao);
    }

    @Test(expected = ArithmeticException.class)
    public void excecaoFalhaAoDividirPorZero() throws Exception {
        int valorA = 6;
        int valorB = 0;
        Calculadora calculadora = new Calculadora();
        int divisao = calculadora.divide(valorA, valorB);

        assertEquals(0, divisao);
    }
}
// teste
