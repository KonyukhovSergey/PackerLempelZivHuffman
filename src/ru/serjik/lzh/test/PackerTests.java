package ru.serjik.lzh.test;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import ru.serjik.lzh.PackerLZH;

public class PackerTests
{
	private static final Random rnd = new Random(System.currentTimeMillis());
	private PackerLZH packer = new PackerLZH();

	private byte[] generateRandomData(int length)
	{
		byte[] data = new byte[length];
		rnd.nextBytes(data);
		return data;
	}

	@Test
	public void testEncodeDecodeMuchRandomData()
	{
		for (int i = 0; i < 100; i++)
		{
			byte[] data = generateRandomData(rnd.nextInt(128 * 1024) + 256);
			assertArrayEquals(data, packer.decode(packer.encode(data)));
		}
	}

	@Test
	public void testEncodeDecodeBigData()
	{
		byte[] data = generateRandomData(2 * 1024 * 1024);
		assertArrayEquals(data, packer.decode(packer.encode(data)));
	}

	@Test
	public void testTerminalEncode()
	{
		assertArrayEquals(new byte[] { 0, 0, 0, 0 }, packer.encode(new byte[] {}));
	}

	@Test
	public void testTerminalDecode()
	{
		assertArrayEquals(new byte[] {}, packer.decode(new byte[] { 0, 0, 0, 0 }));
	}

}
