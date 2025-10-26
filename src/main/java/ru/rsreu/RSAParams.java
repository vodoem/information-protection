package ru.rsreu;

import java.math.BigInteger;

public record RSAParams(BigInteger n, BigInteger phi, BigInteger e, BigInteger d) {}
