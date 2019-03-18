package ru.serjik.lzh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// source from https://github.com/msmiley/lzh/blob/master/src/lzh.c

public class PackerLZH
{
	private final static int N = 4096; /* buffer size */
	private final static int F = 60; /* look ahead buffer size */
	private final static int THRESHOLD = 2;
	private final static int NIL = N; /* leaf of tree */

	private int[] buffer = new int[N + F - 1];
	private int matchPosition, matchLength;
	private int[] leftChild = new int[N + 1];
	private int[] rightChild = new int[N + 257];
	private int[] parent = new int[N + 1];

	private void initTree()
	{
		int i;

		for (i = N + 1; i <= N + 256; i++)
			rightChild[i] = NIL;
		for (i = 0; i < N; i++)
			parent[i] = NIL;
	}

	private void insertNode(int r)
	{
		int i, p, cmp;
		int offset;
		int c;

		cmp = 1;
		offset = r;
		p = N + 1 + buffer[offset + 0];
		rightChild[r] = leftChild[r] = NIL;
		matchLength = 0;
		for (;;)
		{
			if (cmp >= 0)
			{
				if (rightChild[p] != NIL)
				{
					p = rightChild[p];
				}
				else
				{
					rightChild[p] = r;
					parent[r] = p;
					return;
				}
			}
			else
			{
				if (leftChild[p] != NIL)
				{
					p = leftChild[p];
				}
				else
				{
					leftChild[p] = r;
					parent[r] = p;
					return;
				}
			}
			for (i = 1; i < F; i++)
			{
				if ((cmp = buffer[offset + i] - buffer[p + i]) != 0)
				{
					break;
				}
			}
			if (i > THRESHOLD)
			{
				if (i > matchLength)
				{
					matchPosition = ((r - p) & (N - 1)) - 1;

					if ((matchLength = i) >= F)
					{
						break;
					}
				}
				if (i == matchLength)
				{
					if ((c = ((r - p) & (N - 1)) - 1) < matchPosition)
					{
						matchPosition = c;
					}
				}
			}
		}
		parent[r] = parent[p];
		leftChild[r] = leftChild[p];
		rightChild[r] = rightChild[p];
		parent[leftChild[p]] = r;
		parent[rightChild[p]] = r;
		if (rightChild[parent[p]] == p)
		{
			rightChild[parent[p]] = r;
		}
		else
		{
			leftChild[parent[p]] = r;
		}
		parent[p] = NIL;
	}

	private void deleteNode(int p)
	{
		int q;

		if (parent[p] == NIL)
		{
			return;
		}
		if (rightChild[p] == NIL)
		{
			q = leftChild[p];
		}
		else
		{
			if (leftChild[p] == NIL)
			{
				q = rightChild[p];
			}
			else
			{
				q = leftChild[p];
				if (rightChild[q] != NIL)
				{
					do
					{
						q = rightChild[q];
					}
					while (rightChild[q] != NIL);

					rightChild[parent[q]] = leftChild[q];
					parent[leftChild[q]] = parent[q];
					leftChild[q] = leftChild[p];
					parent[leftChild[p]] = q;
				}
				rightChild[q] = rightChild[p];
				parent[rightChild[p]] = q;
			}
		}
		parent[q] = parent[p];
		if (rightChild[parent[p]] == p)
		{
			rightChild[parent[p]] = q;
		}
		else
		{
			leftChild[parent[p]] = q;
		}
		parent[p] = NIL;
	}

	private final static int N_CHAR = (256 - THRESHOLD + F);
	private final static int T = (N_CHAR * 2 - 1);
	private final static int R = (T - 1);
	private final static int MAX_FREQ = 0x8000;

	private final static int[] p_len = new int[] { 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
			0x05, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x07, 0x07, 0x07, 0x07, 0x07,
			0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
			0x07, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08 };

	private final static int[] p_code = new int[] { 0x00, 0x20, 0x30, 0x40, 0x50, 0x58, 0x60, 0x68, 0x70, 0x78, 0x80,
			0x88, 0x90, 0x94, 0x98, 0x9C, 0xA0, 0xA4, 0xA8, 0xAC, 0xB0, 0xB4, 0xB8, 0xBC, 0xC0, 0xC2, 0xC4, 0xC6, 0xC8,
			0xCA, 0xCC, 0xCE, 0xD0, 0xD2, 0xD4, 0xD6, 0xD8, 0xDA, 0xDC, 0xDE, 0xE0, 0xE2, 0xE4, 0xE6, 0xE8, 0xEA, 0xEC,
			0xEE, 0xF0, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8, 0xF9, 0xFA, 0xFB, 0xFC, 0xFD, 0xFE, 0xFF };

	/* for decoding */
	private final static int[] d_code = new int[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
			0x01, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
			0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04,
			0x04, 0x04, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x06, 0x06, 0x06, 0x06, 0x06,
			0x06, 0x06, 0x06, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
			0x08, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x0A, 0x0A, 0x0A, 0x0A, 0x0A, 0x0A, 0x0A, 0x0A, 0x0B,
			0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0C, 0x0C, 0x0C, 0x0C, 0x0D, 0x0D, 0x0D, 0x0D, 0x0E, 0x0E, 0x0E,
			0x0E, 0x0F, 0x0F, 0x0F, 0x0F, 0x10, 0x10, 0x10, 0x10, 0x11, 0x11, 0x11, 0x11, 0x12, 0x12, 0x12, 0x12, 0x13,
			0x13, 0x13, 0x13, 0x14, 0x14, 0x14, 0x14, 0x15, 0x15, 0x15, 0x15, 0x16, 0x16, 0x16, 0x16, 0x17, 0x17, 0x17,
			0x17, 0x18, 0x18, 0x19, 0x19, 0x1A, 0x1A, 0x1B, 0x1B, 0x1C, 0x1C, 0x1D, 0x1D, 0x1E, 0x1E, 0x1F, 0x1F, 0x20,
			0x20, 0x21, 0x21, 0x22, 0x22, 0x23, 0x23, 0x24, 0x24, 0x25, 0x25, 0x26, 0x26, 0x27, 0x27, 0x28, 0x28, 0x29,
			0x29, 0x2A, 0x2A, 0x2B, 0x2B, 0x2C, 0x2C, 0x2D, 0x2D, 0x2E, 0x2E, 0x2F, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34,
			0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, };

	private final static int[] d_len = new int[] { 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
			0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
			0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
			0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
			0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05,
			0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
			0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
			0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
			0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
			0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
			0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
			0x06, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
			0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
			0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x08, 0x08, 0x08, 0x08, 0x08,
			0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, };

	private int[] freq = new int[T + 1];

	private int[] prnt = new int[T + N_CHAR];

	private int[] son = new int[T];

	private int getbuf = 0;
	private int getlen = 0;

	private OutputStream outputStream;
	private InputStream inputStream;

	private int getBit() throws IOException
	{
		int i;

		while (getlen <= 8)
		{
			i = inputStream.read();

			if (i < 0)
			{
				i = 0;
			}

			getbuf |= i << (8 - getlen);
			getlen += 8;
		}
		i = getbuf;
		getbuf <<= 1;
		getlen--;
		return ((i & 0x8000) >> 15);
	}

	private int getByte() throws IOException
	{
		int i;

		while (getlen <= 8)
		{
			i = inputStream.read();

			if (i < 0)
			{
				i = 0;
			}

			getbuf |= i << (8 - getlen);
			getlen += 8;
		}
		i = getbuf & 0xffff;
		getbuf <<= 8;
		getlen -= 8;
		return ((i & 0xff00) >> 8);
	}

	private int putbuf = 0;
	private int putlen = 0;

	private void putCode(int l, int c) throws IOException
	{
		putbuf |= c >> putlen;
		if ((putlen += l) >= 8)
		{
			outputStream.write(putbuf >> 8);
			if ((putlen -= 8) >= 8)
			{
				outputStream.write(putbuf);
				putlen -= 8;
				putbuf = (c << (l - putlen));
			}
			else
				putbuf = (putbuf << 8);
		}
	}

	private void startHuff()
	{
		int i, j;

		for (i = 0; i < N_CHAR; i++)
		{
			freq[i] = 1;
			son[i] = i + T;
			prnt[i + T] = i;
		}
		i = 0;
		j = N_CHAR;
		while (j <= R)
		{
			freq[j] = freq[i] + freq[i + 1];
			son[j] = i;
			prnt[i] = prnt[i + 1] = j;
			i += 2;
			j++;
		}
		freq[T] = 0xffff;
		prnt[R] = 0;
	}

	private void reconst()
	{
		int i, j, k;
		int f, l;

		j = 0;
		for (i = 0; i < T; i++)
		{
			if (son[i] >= T)
			{
				freq[j] = (freq[i] + 1) / 2;
				son[j] = son[i];
				j++;
			}
		}
		for (i = 0, j = N_CHAR; j < T; i += 2, j++)
		{
			k = i + 1;
			f = freq[j] = freq[i] + freq[k];
			for (k = j - 1; f < freq[k]; k--)
				;
			k++;
			l = (j - k) * 2;
			// memmove(&freq[k + 1], &freq[k], l);
			System.arraycopy(freq, k, freq, k + 1, l);
			// move(freq, k, l);
			freq[k] = f;
			// memmove(&son[k + 1], &son[k], l);
			System.arraycopy(son, k, son, k + 1, l);
			// move(son, k, l);
			son[k] = i;
		}
		for (i = 0; i < T; i++)
		{
			if ((k = son[i]) >= T)
			{
				prnt[k] = i;
			}
			else
			{
				prnt[k] = prnt[k + 1] = i;
			}
		}
	}

	private void move(int[] array, int start, int lenght)
	{
		for (int i = start + lenght; i > start; i--)
		{
			array[i] = array[i - 1];
		}
	}

	/* increment frequency of given code by one, and update tree */
	private void update(int c)
	{
		int i, j, k, l;

		if (freq[R] == MAX_FREQ)
		{
			reconst();
		}

		c = prnt[c + T];
		do
		{
			k = ++freq[c];

			if (k > freq[l = c + 1])
			{
				while (k > freq[++l])
					;
				l--;
				freq[c] = freq[l];
				freq[l] = k;

				i = son[c];
				prnt[i] = l;
				if (i < T)
					prnt[i + 1] = l;

				j = son[l];
				son[l] = i;

				prnt[j] = c;
				if (j < T)
					prnt[j + 1] = c;
				son[c] = j;

				c = l;
			}
		}
		while ((c = prnt[c]) != 0);
	}

	int code, len;

	private void encodeChar(int c) throws IOException
	{
		int i;
		int j, k;

		i = 0;
		j = 0;
		k = prnt[c + T];

		do
		{
			i >>= 1;

			if ((k & 1) != 0)
			{
				i += 0x8000;
			}

			j++;
		}
		while ((k = prnt[k]) != R);
		putCode(j, i);
		code = i;
		len = j;
		update(c);
	}

	private void encodePosition(int c) throws IOException
	{
		int i = c >> 6;
		putCode(p_len[i], p_code[i] << 8);
		putCode(6, (c & 0x3f) << 10);
	}

	private void encodeEnd() throws IOException
	{
		if (putlen != 0)
		{
			outputStream.write(putbuf >> 8);
		}
	}

	private int decodeChar() throws IOException
	{
		int c;

		c = son[R];

		while (c < T)
		{
			c += getBit();
			c = son[c];
		}

		c -= T;
		update(c);

		return c;
	}

	private int decodePosition() throws IOException
	{
		int i, j, c;

		i = getByte();
		c = (int) d_code[i] << 6;
		j = d_len[i];

		/* read lower 6 bits verbatim */
		j -= 2;
		while (j-- != 0)
		{
			i = (i << 1) + getBit();
		}
		return c | (i & 0x3f);
	}

	private static void write(int value, OutputStream stream) throws IOException
	{
		stream.write((byte) (value & 0xff));
		value >>= 8;
		stream.write((byte) (value & 0xff));
		value >>= 8;
		stream.write((byte) (value & 0xff));
		value >>= 8;
		stream.write((byte) value);
	}

	public byte[] encode(byte[] data)
	{
		try
		{
			initValues();
			outputStream = new ByteArrayOutputStream();

			int i, c, len, r, s, last_match_length;
			int offset = 0;

			int inlen = data.length;

			if (inlen == 0)
				return new byte[] { 0, 0, 0, 0 };

			write(inlen, outputStream);

			startHuff();
			initTree();
			s = 0;
			r = N - F;
			for (i = s; i < r; i++)
			{
				buffer[i] = ' ';
			}

			for (len = 0; len < F && len < inlen; len++)
			{
				buffer[r + len] = ((int) data[offset++] & 0xff);
			}

			for (i = 1; i <= F; i++)
			{
				insertNode(r - i);
			}

			insertNode(r);

			do
			{
				if (matchLength > len)
				{
					matchLength = len;
				}

				if (matchLength <= THRESHOLD)
				{
					matchLength = 1;
					encodeChar(buffer[r]);
				}
				else
				{
					encodeChar(255 - THRESHOLD + matchLength);
					encodePosition(matchPosition);
				}
				last_match_length = matchLength;
				for (i = 0; i < last_match_length && offset < inlen; i++)
				{
					c = ((int) data[offset++] & 0xff);
					deleteNode(s);
					buffer[s] = c;
					if (s < F - 1)
					{
						buffer[s + N] = c;
					}
					s = (s + 1) & (N - 1);
					r = (r + 1) & (N - 1);
					insertNode(r);
				}
				while (i++ < last_match_length)
				{
					deleteNode(s);
					s = (s + 1) & (N - 1);
					r = (r + 1) & (N - 1);
					if (--len != 0)
					{
						insertNode(r);
					}
				}
			}
			while (len > 0);

			encodeEnd();

			byte[] result = ((ByteArrayOutputStream) outputStream).toByteArray();
			outputStream.close();
			// inputStream.close();
			return result;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return new byte[] { 0, 0, 0, 0 };
		}
	}

	private static int readInt(InputStream inputStream) throws IOException
	{
		int value = (inputStream.read() & 0xff);
		value |= (inputStream.read() % 0xff) << 8;
		value |= (inputStream.read() % 0xff) << 16;
		value |= (inputStream.read() % 0xff) << 24;
		return value;
	}

	public byte[] decode(byte[] data)
	{
		try
		{
			int c, count;
			int i, j, k, r;

			initValues();

			inputStream = new ByteArrayInputStream(data);

			int textsize = readInt(inputStream);
			// input += 4;

			if (textsize == 0)
				return new byte[0];

			byte[] out = new byte[textsize];

			startHuff();

			for (i = 0; i < N - F; i++)
            {
				buffer[i] = ' ';
            }

			r = N - F;
			for (count = 0; count < textsize;)
			{
				c = decodeChar();
				if (c < 256)
				{
					out[count] = (byte) (c & 0xff);
					buffer[r++] = (byte) (c & 0xff);
					r &= (N - 1);
					count++;
				}
				else
				{
					i = (r - decodePosition() - 1) & (N - 1);
					j = c - 255 + THRESHOLD;
					for (k = 0; k < j; k++)
					{
						c = buffer[(i + k) & (N - 1)];
						out[count] = (byte) (c & 0xff);
						buffer[r++] = (byte) (c & 0xff);
						r &= (N - 1);
						count++;
					}
				}
			}

			return out;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return new byte[0];
		}
	}

	private void initValues()
	{
		putbuf = 0;
		putlen = 0;
		getbuf = 0;
		getlen = 0;
		code = 0;
		len = 0;
	}
}
