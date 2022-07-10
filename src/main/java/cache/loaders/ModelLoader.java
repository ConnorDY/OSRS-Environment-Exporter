/*
Adapted from RuneLite source code, which provides this to us under the following
license:

BSD 2-Clause License

Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package cache.loaders;

import cache.IndexType;
import cache.definitions.ModelDefinition;
import cache.utils.ByteBufferExtKt;
import com.displee.cache.CacheLibrary;
import com.displee.cache.index.Index;
import com.displee.cache.index.archive.Archive;
import com.displee.cache.index.archive.file.File;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ModelLoader
{
	private final CacheLibrary cacheLibrary;
	private final Map<Integer, ModelDefinition> modelDefinitionCache;

	public ModelLoader(CacheLibrary cacheLibrary) {
		this.cacheLibrary = cacheLibrary;
		this.modelDefinitionCache = new HashMap<>();
	}

	public ModelDefinition get(int modelId) throws IOException
	{
		ModelDefinition def = modelDefinitionCache.get(modelId);
		if (def != null) return new ModelDefinition(def, false, false, false);

		Index index = cacheLibrary.index(IndexType.MODELS.getId());
		Archive archive = index.archive(modelId & 0xffff);
		if (archive == null) return null;

		File file = archive.getFiles().get(0);
		byte[] b = file.getData();
		archive.restore();  // Drop cached archive

		def = new ModelDefinition();
		def.setId(modelId);

		if (b[b.length - 1] == -3 && b[b.length - 2] == -1)
		{
			decodeType3(def, b);
		}
		else if (b[b.length - 1] == -2 && b[b.length - 2] == -1)
		{
			decodeType2(def, b);
		}
		else if (b[b.length - 1] == -1 && b[b.length - 2] == -1)
		{
			decodeType1(def, b);
		}
		else
		{
			decodeOldFormat(def, b);
		}

		def.computeNormals();
		def.computeTextureUVCoordinates();
		def.computeAnimationTables();
		modelDefinitionCache.put(modelId, def);

		return new ModelDefinition(def, false, false, false);
	}

	private void decodeType3(ModelDefinition def, byte[] var1) throws IOException {
		ByteBuffer var2 = ByteBuffer.wrap(var1);
		ByteBuffer var3 = ByteBuffer.wrap(var1);
		ByteBuffer var4 = ByteBuffer.wrap(var1);
		ByteBuffer var5 = ByteBuffer.wrap(var1);
		ByteBuffer var6 = ByteBuffer.wrap(var1);
		ByteBuffer var7 = ByteBuffer.wrap(var1);
		ByteBuffer var8 = ByteBuffer.wrap(var1);
		var2.position(var1.length - 26);
		int var9 = ByteBufferExtKt.readUnsignedShort(var2);
		int var10 = ByteBufferExtKt.readUnsignedShort(var2);
		int var11 = ByteBufferExtKt.readUnsignedByte(var2);
		int var12 = ByteBufferExtKt.readUnsignedByte(var2);
		int var13 = ByteBufferExtKt.readUnsignedByte(var2);
		int var14 = ByteBufferExtKt.readUnsignedByte(var2);
		int var15 = ByteBufferExtKt.readUnsignedByte(var2);
		int var16 = ByteBufferExtKt.readUnsignedByte(var2);
		int var17 = ByteBufferExtKt.readUnsignedByte(var2);
		int var18 = ByteBufferExtKt.readUnsignedByte(var2);
		int var19 = ByteBufferExtKt.readUnsignedShort(var2);
		int var20 = ByteBufferExtKt.readUnsignedShort(var2);
		int var21 = ByteBufferExtKt.readUnsignedShort(var2);
		int var22 = ByteBufferExtKt.readUnsignedShort(var2);
		int var23 = ByteBufferExtKt.readUnsignedShort(var2);
		int var24 = ByteBufferExtKt.readUnsignedShort(var2);
		int var25 = 0;
		int var26 = 0;
		int var27 = 0;
		int var28;
		if (var11 > 0)
		{
			def.setTextureRenderTypes(new byte[var11]);
			var2.rewind();

			for (var28 = 0; var28 < var11; ++var28)
			{
				byte var29 = def.getTextureRenderTypes()[var28] = var2.get();
				if (var29 == 0)
				{
					++var25;
				}

				if (var29 >= 1 && var29 <= 3)
				{
					++var26;
				}

				if (var29 == 2)
				{
					++var27;
				}
			}
		}

		var28 = var11 + var9;
		int var58 = var28;
		if (var12 == 1)
		{
			var28 += var10;
		}

		int var30 = var28;
		var28 += var10;
		int var31 = var28;
		if (var13 == 255)
		{
			var28 += var10;
		}

		int var32 = var28;
		if (var15 == 1)
		{
			var28 += var10;
		}

		int var33 = var28;
		var28 += var24;
		int var34 = var28;
		if (var14 == 1)
		{
			var28 += var10;
		}

		int var35 = var28;
		var28 += var22;
		int var36 = var28;
		if (var16 == 1)
		{
			var28 += var10 * 2;
		}

		int var37 = var28;
		var28 += var23;
		int var38 = var28;
		var28 += var10 * 2;
		int var39 = var28;
		var28 += var19;
		int var40 = var28;
		var28 += var20;
		int var41 = var28;
		var28 += var21;
		int var42 = var28;
		var28 += var25 * 6;
		int var43 = var28;
		var28 += var26 * 6;
		int var44 = var28;
		var28 += var26 * 6;
		int var45 = var28;
		var28 += var26 * 2;
		int var46 = var28;
		var28 += var26;
		int var47 = var28;
		var28 = var28 + var26 * 2 + var27 * 2;
		def.setVertexCount(var9);
		def.setFaceCount(var10);
		def.setTextureTriangleCount(var11);
		def.setVertexPositionsX(new int[var9]);
		def.setVertexPositionsY(new int[var9]);
		def.setVertexPositionsZ(new int[var9]);
		def.setFaceVertexIndices1(new int[var10]);
		def.setFaceVertexIndices2(new int[var10]);
		def.setFaceVertexIndices3(new int[var10]);
		if (var17 == 1)
		{
			def.setVertexSkins(new int[var9]);
		}

		if (var12 == 1)
		{
			def.setFaceRenderTypes(new byte[var10]);
		}

		if (var13 == 255)
		{
			def.setFaceRenderPriorities(new byte[var10]);
		}
		else
		{
			def.setPriority((byte) var13);
		}

		if (var14 == 1)
		{
			def.setFaceAlphas(new byte[var10]);
		}

		if (var15 == 1)
		{
			def.setFaceSkins(new int[var10]);
		}

		if (var16 == 1)
		{
			def.setFaceTextures(new short[var10]);
		}

		if (var16 == 1 && var11 > 0)
		{
			def.setTextureCoordinates(new byte[var10]);
		}

		if (var18 == 1)
		{
//			def.setAnimayaGroups(new int[var9][]);
//			def.setAnimayaScales(new int[var9][]);
		}

		def.setFaceColors(new short[var10]);
		if (var11 > 0)
		{
			def.setTextureTriangleVertexIndices1(new short[var11]);
			def.setTextureTriangleVertexIndices2(new short[var11]);
			def.setTextureTriangleVertexIndices3(new short[var11]);
		}

		var2.position(var11);
		var3.position(var39);
		var4.position(var40);
		var5.position(var41);
		var6.position(var33);
		int var48 = 0;
		int var49 = 0;
		int var50 = 0;

		int var51;
		int var52;
		int var53;
		int var54;
		int var55;
		for (var51 = 0; var51 < var9; ++var51)
		{
			var52 = ByteBufferExtKt.readUnsignedByte(var2);
			var53 = 0;
			if ((var52 & 1) != 0)
			{
				var53 = ByteBufferExtKt.readShortSmart(var3);
			}

			var54 = 0;
			if ((var52 & 2) != 0)
			{
				var54 = ByteBufferExtKt.readShortSmart(var4);
			}

			var55 = 0;
			if ((var52 & 4) != 0)
			{
				var55 = ByteBufferExtKt.readShortSmart(var5);
			}

			def.getVertexPositionsX()[var51] = var48 + var53;
			def.getVertexPositionsY()[var51] = var49 + var54;
			def.getVertexPositionsZ()[var51] = var50 + var55;
			var48 = def.getVertexPositionsX()[var51];
			var49 = def.getVertexPositionsY()[var51];
			var50 = def.getVertexPositionsZ()[var51];
			if (var17 == 1)
			{
				def.getVertexSkins()[var51] = ByteBufferExtKt.readUnsignedByte(var6);
			}
		}

		if (var18 == 1)
		{
			for (var51 = 0; var51 < var9; ++var51)
			{
				var52 = ByteBufferExtKt.readUnsignedByte(var6);
//				def.getAnimayaGroups()[var51] = new int[var52];
//				def.getAnimayaScales()[var51] = new int[var52];

				for (var53 = 0; var53 < var52; ++var53)
				{
					/*def.getAnimayaGroups()[var51][var53] =*/ ByteBufferExtKt.readUnsignedByte(var6);
					/*def.getAnimayaScales()[var51][var53] =*/ ByteBufferExtKt.readUnsignedByte(var6);
				}
			}
		}

		var2.position(var38);
		var3.position(var58);
		var4.position(var31);
		var5.position(var34);
		var6.position(var32);
		var7.position(var36);
		var8.position(var37);

		for (var51 = 0; var51 < var10; ++var51)
		{
			def.getFaceColors()[var51] = (short) ByteBufferExtKt.readUnsignedShort(var2);
			if (var12 == 1)
			{
				def.getFaceRenderTypes()[var51] = var3.get();
			}

			if (var13 == 255)
			{
				def.getFaceRenderPriorities()[var51] = var4.get();
			}

			if (var14 == 1)
			{
				def.getFaceAlphas()[var51] = var5.get();
			}

			if (var15 == 1)
			{
				def.getFaceSkins()[var51] = ByteBufferExtKt.readUnsignedByte(var6);
			}

			if (var16 == 1)
			{
				def.getFaceTextures()[var51] = (short) (ByteBufferExtKt.readUnsignedShort(var7) - 1);
			}

			if (def.getTextureCoordinates() != null && def.getFaceTextures()[var51] != -1)
			{
				def.getTextureCoordinates()[var51] = (byte) (ByteBufferExtKt.readUnsignedByte(var8) - 1);
			}
		}

		var2.position(var35);
		var3.position(var30);
		var51 = 0;
		var52 = 0;
		var53 = 0;
		var54 = 0;

		int var56;
		for (var55 = 0; var55 < var10; ++var55)
		{
			var56 = ByteBufferExtKt.readUnsignedByte(var3);
			if (var56 == 1)
			{
				var51 = ByteBufferExtKt.readShortSmart(var2) + var54;
				var52 = ByteBufferExtKt.readShortSmart(var2) + var51;
				var53 = ByteBufferExtKt.readShortSmart(var2) + var52;
				var54 = var53;
				def.getFaceVertexIndices1()[var55] = var51;
				def.getFaceVertexIndices2()[var55] = var52;
				def.getFaceVertexIndices3()[var55] = var53;
			}

			if (var56 == 2)
			{
				var52 = var53;
				var53 = ByteBufferExtKt.readShortSmart(var2) + var54;
				var54 = var53;
				def.getFaceVertexIndices1()[var55] = var51;
				def.getFaceVertexIndices2()[var55] = var52;
				def.getFaceVertexIndices3()[var55] = var53;
			}

			if (var56 == 3)
			{
				var51 = var53;
				var53 = ByteBufferExtKt.readShortSmart(var2) + var54;
				var54 = var53;
				def.getFaceVertexIndices1()[var55] = var51;
				def.getFaceVertexIndices2()[var55] = var52;
				def.getFaceVertexIndices3()[var55] = var53;
			}

			if (var56 == 4)
			{
				int var57 = var51;
				var51 = var52;
				var52 = var57;
				var53 = ByteBufferExtKt.readShortSmart(var2) + var54;
				var54 = var53;
				def.getFaceVertexIndices1()[var55] = var51;
				def.getFaceVertexIndices2()[var55] = var57;
				def.getFaceVertexIndices3()[var55] = var53;
			}
		}

		var2.position(var42);
		var3.position(var43);
		var4.position(var44);
		var5.position(var45);
		var6.position(var46);
		var7.position(var47);

		for (var55 = 0; var55 < var11; ++var55)
		{
			var56 = def.getTextureRenderTypes()[var55] & 255;
			if (var56 == 0)
			{
				
				def.getTextureTriangleVertexIndices1()[var55] = (short) ByteBufferExtKt.readUnsignedShort(var2);
				def.getTextureTriangleVertexIndices2()[var55] = (short) ByteBufferExtKt.readUnsignedShort(var2);
				def.getTextureTriangleVertexIndices3()[var55] = (short) ByteBufferExtKt.readUnsignedShort(var2);
			}
		}

		var2.position(var28);
		var55 = ByteBufferExtKt.readUnsignedByte(var2);
		if (var55 != 0)
		{
			ByteBufferExtKt.readUnsignedShort(var2);
			ByteBufferExtKt.readUnsignedShort(var2);
			ByteBufferExtKt.readUnsignedShort(var2);
			ByteBufferExtKt.read24BitInt(var2);
		}

	}

	private void decodeType2(ModelDefinition def, byte[] var1)
	{
		boolean var2 = false;
		boolean var3 = false;
		ByteBuffer var4 = ByteBuffer.wrap(var1);
		ByteBuffer var5 = ByteBuffer.wrap(var1);
		ByteBuffer var6 = ByteBuffer.wrap(var1);
		ByteBuffer var7 = ByteBuffer.wrap(var1);
		ByteBuffer var8 = ByteBuffer.wrap(var1);
		var4.position(var1.length - 23);
		int var9 = ByteBufferExtKt.readUnsignedShort(var4);
		int var10 = ByteBufferExtKt.readUnsignedShort(var4);
		int var11 = ByteBufferExtKt.readUnsignedByte(var4);
		int var12 = ByteBufferExtKt.readUnsignedByte(var4);
		int var13 = ByteBufferExtKt.readUnsignedByte(var4);
		int var14 = ByteBufferExtKt.readUnsignedByte(var4);
		int var15 = ByteBufferExtKt.readUnsignedByte(var4);
		int var16 = ByteBufferExtKt.readUnsignedByte(var4);
		int var17 = ByteBufferExtKt.readUnsignedByte(var4);
		int var18 = ByteBufferExtKt.readUnsignedShort(var4);
		int var19 = ByteBufferExtKt.readUnsignedShort(var4);
		int var20 = ByteBufferExtKt.readUnsignedShort(var4);
		int var21 = ByteBufferExtKt.readUnsignedShort(var4);
		int var22 = ByteBufferExtKt.readUnsignedShort(var4);
		byte var23 = 0;
		int var24 = var23 + var9;
		int var25 = var24;
		var24 += var10;
		int var26 = var24;
		if (var13 == 255)
		{
			var24 += var10;
		}

		int var27 = var24;
		if (var15 == 1)
		{
			var24 += var10;
		}

		int var28 = var24;
		if (var12 == 1)
		{
			var24 += var10;
		}

		int var29 = var24;
		var24 += var22;
		int var30 = var24;
		if (var14 == 1)
		{
			var24 += var10;
		}

		int var31 = var24;
		var24 += var21;
		int var32 = var24;
		var24 += var10 * 2;
		int var33 = var24;
		var24 += var11 * 6;
		int var34 = var24;
		var24 += var18;
		int var35 = var24;
		var24 += var19;
		int var10000 = var24 + var20;
		def.setVertexCount(var9);
		def.setFaceCount(var10);
		def.setTextureTriangleCount(var11);
		def.setVertexPositionsX(new int[var9]);
		def.setVertexPositionsY(new int[var9]);
		def.setVertexPositionsZ(new int[var9]);
		def.setFaceVertexIndices1(new int[var10]);
		def.setFaceVertexIndices2(new int[var10]);
		def.setFaceVertexIndices3(new int[var10]);
		if (var11 > 0)
		{
			def.setTextureRenderTypes(new byte[var11]);
			def.setTextureTriangleVertexIndices1(new short[var11]);
			def.setTextureTriangleVertexIndices2(new short[var11]);
			def.setTextureTriangleVertexIndices3(new short[var11]);
		}

		if (var16 == 1)
		{
			def.setVertexSkins(new int[var9]);
		}

		if (var12 == 1)
		{
			def.setFaceRenderTypes(new byte[var10]);
			def.setTextureCoordinates(new byte[var10]);
			def.setFaceTextures(new short[var10]);
		}

		if (var13 == 255)
		{
			def.setFaceRenderPriorities(new byte[var10]);
		}
		else
		{
			def.setPriority((byte) var13);
		}

		if (var14 == 1)
		{
			def.setFaceAlphas(new byte[var10]);
		}

		if (var15 == 1)
		{
			def.setFaceSkins(new int[var10]);
		}

		if (var17 == 1)
		{
//			def.setAnimayaGroups(new int[var9][]);
//			def.setAnimayaScales(new int[var9][]);
		}

		def.setFaceColors(new short[var10]);
		var4.position(var23);
		var5.position(var34);
		var6.position(var35);
		var7.position(var24);
		var8.position(var29);
		int var37 = 0;
		int var38 = 0;
		int var39 = 0;

		int var40;
		int var41;
		int var42;
		int var43;
		int var44;
		for (var40 = 0; var40 < var9; ++var40)
		{
			var41 = ByteBufferExtKt.readUnsignedByte(var4);
			var42 = 0;
			if ((var41 & 1) != 0)
			{
				var42 = ByteBufferExtKt.readShortSmart(var5);
			}

			var43 = 0;
			if ((var41 & 2) != 0)
			{
				var43 = ByteBufferExtKt.readShortSmart(var6);
			}

			var44 = 0;
			if ((var41 & 4) != 0)
			{
				var44 = ByteBufferExtKt.readShortSmart(var7);
			}

			def.getVertexPositionsX()[var40] = var37 + var42;
			def.getVertexPositionsY()[var40] = var38 + var43;
			def.getVertexPositionsZ()[var40] = var39 + var44;
			var37 = def.getVertexPositionsX()[var40];
			var38 = def.getVertexPositionsY()[var40];
			var39 = def.getVertexPositionsZ()[var40];
			if (var16 == 1)
			{
				def.getVertexSkins()[var40] = ByteBufferExtKt.readUnsignedByte(var8);
			}
		}

		if (var17 == 1)
		{
			for (var40 = 0; var40 < var9; ++var40)
			{
				var41 = ByteBufferExtKt.readUnsignedByte(var8);
//				def.getAnimayaGroups()[var40] = new int[var41];
//				def.getAnimayaScales()[var40] = new int[var41];

				for (var42 = 0; var42 < var41; ++var42)
				{
					/*def.getAnimayaGroups()[var40][var42] =*/ ByteBufferExtKt.readUnsignedByte(var8);
					/*def.getAnimayaScales()[var40][var42] =*/ ByteBufferExtKt.readUnsignedByte(var8);
				}
			}
		}

		var4.position(var32);
		var5.position(var28);
		var6.position(var26);
		var7.position(var30);
		var8.position(var27);

		for (var40 = 0; var40 < var10; ++var40)
		{
			def.getFaceColors()[var40] = (short) ByteBufferExtKt.readUnsignedShort(var4);
			if (var12 == 1)
			{
				var41 = ByteBufferExtKt.readUnsignedByte(var5);
				if ((var41 & 1) == 1)
				{
					def.getFaceRenderTypes()[var40] = 1;
					var2 = true;
				}
				else
				{
					def.getFaceRenderTypes()[var40] = 0;
				}

				if ((var41 & 2) == 2)
				{
					def.getTextureCoordinates()[var40] = (byte) (var41 >> 2);
					def.getFaceTextures()[var40] = def.getFaceColors()[var40];
					def.getFaceColors()[var40] = 127;
					if (def.getFaceTextures()[var40] != -1)
					{
						var3 = true;
					}
				}
				else
				{
					def.getTextureCoordinates()[var40] = -1;
					def.getFaceTextures()[var40] = -1;
				}
			}

			if (var13 == 255)
			{
				def.getFaceRenderPriorities()[var40] = var6.get();
			}

			if (var14 == 1)
			{
				def.getFaceAlphas()[var40] = var7.get();
			}

			if (var15 == 1)
			{
				def.getFaceSkins()[var40] = ByteBufferExtKt.readUnsignedByte(var8);
			}
		}

		var4.position(var31);
		var5.position(var25);
		var40 = 0;
		var41 = 0;
		var42 = 0;
		var43 = 0;

		int var45;
		int var46;
		for (var44 = 0; var44 < var10; ++var44)
		{
			var45 = ByteBufferExtKt.readUnsignedByte(var5);
			if (var45 == 1)
			{
				var40 = ByteBufferExtKt.readShortSmart(var4) + var43;
				var41 = ByteBufferExtKt.readShortSmart(var4) + var40;
				var42 = ByteBufferExtKt.readShortSmart(var4) + var41;
				var43 = var42;
				def.getFaceVertexIndices1()[var44] = var40;
				def.getFaceVertexIndices2()[var44] = var41;
				def.getFaceVertexIndices3()[var44] = var42;
			}

			if (var45 == 2)
			{
				var41 = var42;
				var42 = ByteBufferExtKt.readShortSmart(var4) + var43;
				var43 = var42;
				def.getFaceVertexIndices1()[var44] = var40;
				def.getFaceVertexIndices2()[var44] = var41;
				def.getFaceVertexIndices3()[var44] = var42;
			}

			if (var45 == 3)
			{
				var40 = var42;
				var42 = ByteBufferExtKt.readShortSmart(var4) + var43;
				var43 = var42;
				def.getFaceVertexIndices1()[var44] = var40;
				def.getFaceVertexIndices2()[var44] = var41;
				def.getFaceVertexIndices3()[var44] = var42;
			}

			if (var45 == 4)
			{
				var46 = var40;
				var40 = var41;
				var41 = var46;
				var42 = ByteBufferExtKt.readShortSmart(var4) + var43;
				var43 = var42;
				def.getFaceVertexIndices1()[var44] = var40;
				def.getFaceVertexIndices2()[var44] = var46;
				def.getFaceVertexIndices3()[var44] = var42;
			}
		}

		var4.position(var33);

		for (var44 = 0; var44 < var11; ++var44)
		{
			def.getTextureRenderTypes()[var44] = 0;
			def.getTextureTriangleVertexIndices1()[var44] = (short) ByteBufferExtKt.readUnsignedShort(var4);
			def.getTextureTriangleVertexIndices2()[var44] = (short) ByteBufferExtKt.readUnsignedShort(var4);
			def.getTextureTriangleVertexIndices3()[var44] = (short) ByteBufferExtKt.readUnsignedShort(var4);
		}

		if (def.getTextureCoordinates() != null)
		{
			boolean var47 = false;

			for (var45 = 0; var45 < var10; ++var45)
			{
				var46 = def.getTextureCoordinates()[var45] & 255;
				if (var46 != 255)
				{
					if (def.getFaceVertexIndices1()[var45] == (def.getTextureTriangleVertexIndices1()[var46] & '\uffff') && def.getFaceVertexIndices2()[var45] == (def.getTextureTriangleVertexIndices2()[var46] & '\uffff') && def.getFaceVertexIndices3()[var45] == (def.getTextureTriangleVertexIndices3()[var46] & '\uffff'))
					{
						def.getTextureCoordinates()[var45] = -1;
					}
					else
					{
						var47 = true;
					}
				}
			}

			if (!var47)
			{
				def.setTextureCoordinates(null);
			}
		}

		if (!var3)
		{
			def.setFaceTextures(null);
		}

		if (!var2)
		{
			def.setFaceRenderTypes(null);
		}

	}

	private void decodeType1(ModelDefinition def, byte[] var1)
	{
		ByteBuffer var2 = ByteBuffer.wrap(var1);
		ByteBuffer var3 = ByteBuffer.wrap(var1);
		ByteBuffer var4 = ByteBuffer.wrap(var1);
		ByteBuffer var5 = ByteBuffer.wrap(var1);
		ByteBuffer var6 = ByteBuffer.wrap(var1);
		ByteBuffer var7 = ByteBuffer.wrap(var1);
		ByteBuffer var8 = ByteBuffer.wrap(var1);
		var2.position(var1.length - 23);
		int var9 = ByteBufferExtKt.readUnsignedShort(var2);
		int var10 = ByteBufferExtKt.readUnsignedShort(var2);
		int var11 = ByteBufferExtKt.readUnsignedByte(var2);
		int var12 = ByteBufferExtKt.readUnsignedByte(var2);
		int var13 = ByteBufferExtKt.readUnsignedByte(var2);
		int var14 = ByteBufferExtKt.readUnsignedByte(var2);
		int var15 = ByteBufferExtKt.readUnsignedByte(var2);
		int var16 = ByteBufferExtKt.readUnsignedByte(var2);
		int var17 = ByteBufferExtKt.readUnsignedByte(var2);
		int var18 = ByteBufferExtKt.readUnsignedShort(var2);
		int var19 = ByteBufferExtKt.readUnsignedShort(var2);
		int var20 = ByteBufferExtKt.readUnsignedShort(var2);
		int var21 = ByteBufferExtKt.readUnsignedShort(var2);
		int var22 = ByteBufferExtKt.readUnsignedShort(var2);
		int var23 = 0;
		int var24 = 0;
		int var25 = 0;
		int var26;
		if (var11 > 0)
		{
			def.setTextureRenderTypes(new byte[var11]);
			var2.position(0);

			for (var26 = 0; var26 < var11; ++var26)
			{
				byte var27 = def.getTextureRenderTypes()[var26] = var2.get();
				if (var27 == 0)
				{
					++var23;
				}

				if (var27 >= 1 && var27 <= 3)
				{
					++var24;
				}

				if (var27 == 2)
				{
					++var25;
				}
			}
		}

		var26 = var11 + var9;
		int var56 = var26;
		if (var12 == 1)
		{
			var26 += var10;
		}

		int var28 = var26;
		var26 += var10;
		int var29 = var26;
		if (var13 == 255)
		{
			var26 += var10;
		}

		int var30 = var26;
		if (var15 == 1)
		{
			var26 += var10;
		}

		int var31 = var26;
		if (var17 == 1)
		{
			var26 += var9;
		}

		int var32 = var26;
		if (var14 == 1)
		{
			var26 += var10;
		}

		int var33 = var26;
		var26 += var21;
		int var34 = var26;
		if (var16 == 1)
		{
			var26 += var10 * 2;
		}

		int var35 = var26;
		var26 += var22;
		int var36 = var26;
		var26 += var10 * 2;
		int var37 = var26;
		var26 += var18;
		int var38 = var26;
		var26 += var19;
		int var39 = var26;
		var26 += var20;
		int var40 = var26;
		var26 += var23 * 6;
		int var41 = var26;
		var26 += var24 * 6;
		int var42 = var26;
		var26 += var24 * 6;
		int var43 = var26;
		var26 += var24 * 2;
		int var44 = var26;
		var26 += var24;
		int var45 = var26;
		var26 = var26 + var24 * 2 + var25 * 2;
		def.setVertexCount(var9);
		def.setFaceCount(var10);
		def.setTextureTriangleCount(var11);
		def.setVertexPositionsX(new int[var9]);
		def.setVertexPositionsY(new int[var9]);
		def.setVertexPositionsZ(new int[var9]);
		def.setFaceVertexIndices1(new int[var10]);
		def.setFaceVertexIndices2(new int[var10]);
		def.setFaceVertexIndices3(new int[var10]);
		if (var17 == 1)
		{
			def.setVertexSkins(new int[var9]);
		}

		if (var12 == 1)
		{
			def.setFaceRenderTypes(new byte[var10]);
		}

		if (var13 == 255)
		{
			def.setFaceRenderPriorities(new byte[var10]);
		}
		else
		{
			def.setPriority((byte) var13);
		}

		if (var14 == 1)
		{
			def.setFaceAlphas(new byte[var10]);
		}

		if (var15 == 1)
		{
			def.setFaceSkins(new int[var10]);
		}

		if (var16 == 1)
		{
			def.setFaceTextures(new short[var10]);
		}

		if (var16 == 1 && var11 > 0)
		{
			def.setTextureCoordinates(new byte[var10]);
		}

		def.setFaceColors(new short[var10]);
		if (var11 > 0)
		{
			def.setTextureTriangleVertexIndices1(new short[var11]);
			def.setTextureTriangleVertexIndices2(new short[var11]);
			def.setTextureTriangleVertexIndices3(new short[var11]);
		}

		var2.position(var11);
		var3.position(var37);
		var4.position(var38);
		var5.position(var39);
		var6.position(var31);
		int var46 = 0;
		int var47 = 0;
		int var48 = 0;

		int var49;
		int var50;
		int var51;
		int var52;
		int var53;
		for (var49 = 0; var49 < var9; ++var49)
		{
			var50 = ByteBufferExtKt.readUnsignedByte(var2);
			var51 = 0;
			if ((var50 & 1) != 0)
			{
				var51 = ByteBufferExtKt.readShortSmart(var3);
			}

			var52 = 0;
			if ((var50 & 2) != 0)
			{
				var52 = ByteBufferExtKt.readShortSmart(var4);
			}

			var53 = 0;
			if ((var50 & 4) != 0)
			{
				var53 = ByteBufferExtKt.readShortSmart(var5);
			}

			def.getVertexPositionsX()[var49] = var46 + var51;
			def.getVertexPositionsY()[var49] = var47 + var52;
			def.getVertexPositionsZ()[var49] = var48 + var53;
			var46 = def.getVertexPositionsX()[var49];
			var47 = def.getVertexPositionsY()[var49];
			var48 = def.getVertexPositionsZ()[var49];
			if (var17 == 1)
			{
				def.getVertexSkins()[var49] = ByteBufferExtKt.readUnsignedByte(var6);
			}
		}

		var2.position(var36);
		var3.position(var56);
		var4.position(var29);
		var5.position(var32);
		var6.position(var30);
		var7.position(var34);
		var8.position(var35);

		for (var49 = 0; var49 < var10; ++var49)
		{
			def.getFaceColors()[var49] = (short) ByteBufferExtKt.readUnsignedShort(var2);
			if (var12 == 1)
			{
				def.getFaceRenderTypes()[var49] = var3.get();
			}

			if (var13 == 255)
			{
				def.getFaceRenderPriorities()[var49] = var4.get();
			}

			if (var14 == 1)
			{
				def.getFaceAlphas()[var49] = var5.get();
			}

			if (var15 == 1)
			{
				def.getFaceSkins()[var49] = ByteBufferExtKt.readUnsignedByte(var6);
			}

			if (var16 == 1)
			{
				def.getFaceTextures()[var49] = (short) (ByteBufferExtKt.readUnsignedShort(var7) - 1);
			}

			if (def.getTextureCoordinates() != null && def.getFaceTextures()[var49] != -1)
			{
				def.getTextureCoordinates()[var49] = (byte) (ByteBufferExtKt.readUnsignedByte(var8) - 1);
			}
		}

		var2.position(var33);
		var3.position(var28);
		var49 = 0;
		var50 = 0;
		var51 = 0;
		var52 = 0;

		int var54;
		for (var53 = 0; var53 < var10; ++var53)
		{
			var54 = ByteBufferExtKt.readUnsignedByte(var3);
			if (var54 == 1)
			{
				var49 = ByteBufferExtKt.readShortSmart(var2) + var52;
				var50 = ByteBufferExtKt.readShortSmart(var2) + var49;
				var51 = ByteBufferExtKt.readShortSmart(var2) + var50;
				var52 = var51;
				def.getFaceVertexIndices1()[var53] = var49;
				def.getFaceVertexIndices2()[var53] = var50;
				def.getFaceVertexIndices3()[var53] = var51;
			}

			if (var54 == 2)
			{
				var50 = var51;
				var51 = ByteBufferExtKt.readShortSmart(var2) + var52;
				var52 = var51;
				def.getFaceVertexIndices1()[var53] = var49;
				def.getFaceVertexIndices2()[var53] = var50;
				def.getFaceVertexIndices3()[var53] = var51;
			}

			if (var54 == 3)
			{
				var49 = var51;
				var51 = ByteBufferExtKt.readShortSmart(var2) + var52;
				var52 = var51;
				def.getFaceVertexIndices1()[var53] = var49;
				def.getFaceVertexIndices2()[var53] = var50;
				def.getFaceVertexIndices3()[var53] = var51;
			}

			if (var54 == 4)
			{
				int var55 = var49;
				var49 = var50;
				var50 = var55;
				var51 = ByteBufferExtKt.readShortSmart(var2) + var52;
				var52 = var51;
				def.getFaceVertexIndices1()[var53] = var49;
				def.getFaceVertexIndices2()[var53] = var55;
				def.getFaceVertexIndices3()[var53] = var51;
			}
		}

		var2.position(var40);
		var3.position(var41);
		var4.position(var42);
		var5.position(var43);
		var6.position(var44);
		var7.position(var45);

		for (var53 = 0; var53 < var11; ++var53)
		{
			var54 = def.getTextureRenderTypes()[var53] & 255;
			if (var54 == 0)
			{
				def.getTextureTriangleVertexIndices1()[var53] = (short) ByteBufferExtKt.readUnsignedShort(var2);
				def.getTextureTriangleVertexIndices2()[var53] = (short) ByteBufferExtKt.readUnsignedShort(var2);
				def.getTextureTriangleVertexIndices3()[var53] = (short) ByteBufferExtKt.readUnsignedShort(var2);
			}
		}

		var2.position(var26);
		var53 = ByteBufferExtKt.readUnsignedByte(var2);
		if (var53 != 0)
		{
			ByteBufferExtKt.readUnsignedShort(var2);
			ByteBufferExtKt.readUnsignedShort(var2);
			ByteBufferExtKt.readUnsignedShort(var2);
			ByteBufferExtKt.read24BitInt(var2);
		}

	}

	private void decodeOldFormat(ModelDefinition def, byte[] inputData)
	{
		boolean usesFaceRenderTypes = false;
		boolean usesFaceTextures = false;
		ByteBuffer stream1 = ByteBuffer.wrap(inputData);
		ByteBuffer stream2 = ByteBuffer.wrap(inputData);
		ByteBuffer stream3 = ByteBuffer.wrap(inputData);
		ByteBuffer stream4 = ByteBuffer.wrap(inputData);
		ByteBuffer stream5 = ByteBuffer.wrap(inputData);
		stream1.position(inputData.length - 18);
		int vertexCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int faceCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int textureCount = ByteBufferExtKt.readUnsignedByte(stream1);
		int isTextured = ByteBufferExtKt.readUnsignedByte(stream1);
		int faceRenderPriority = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasFaceTransparencies = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasPackedTransparencyVertexGroups = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasVertexSkins = ByteBufferExtKt.readUnsignedByte(stream1);
		int vertexXDataByteCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int vertexYDataByteCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int vertezZDataByteCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int faceIndexDataByteCount = ByteBufferExtKt.readUnsignedShort(stream1);
		byte offsetOfVertexFlags = 0;
		int dataOffset = offsetOfVertexFlags + vertexCount;
		int offsetOfFaceIndexCompressionTypes = dataOffset;
		dataOffset += faceCount;
		int offsetOfFaceRenderPriorities = dataOffset;
		if (faceRenderPriority == 255)
		{
			dataOffset += faceCount;
		}

		int offsetOfPackedTransparencyVertexGroups = dataOffset;
		if (hasPackedTransparencyVertexGroups == 1)
		{
			dataOffset += faceCount;
		}

		int offsetOfFaceTextureFlags = dataOffset;
		if (isTextured == 1)
		{
			dataOffset += faceCount;
		}

		int offsetOfVertexSkins = dataOffset;
		if (hasVertexSkins == 1)
		{
			dataOffset += vertexCount;
		}

		int offsetOfFaceTransparencies = dataOffset;
		if (hasFaceTransparencies == 1)
		{
			dataOffset += faceCount;
		}

		int offsetOfFaceIndexData = dataOffset;
		dataOffset += faceIndexDataByteCount;
		int offsetOfFaceColorsOrFaceTextures = dataOffset;
		dataOffset += faceCount * 2;
		int offsetOfTextureIndices = dataOffset;
		dataOffset += textureCount * 6;
		int offsetOfVertexXData = dataOffset;
		dataOffset += vertexXDataByteCount;
		int offsetOfVertexYData = dataOffset;
		dataOffset += vertexYDataByteCount;
		int offsetOfVertexZData = dataOffset;
		def.setVertexCount(vertexCount);
		def.setFaceCount(faceCount);
		def.setTextureTriangleCount(textureCount);
		def.setVertexPositionsX(new int[vertexCount]);
		def.setVertexPositionsY(new int[vertexCount]);
		def.setVertexPositionsZ(new int[vertexCount]);
		def.setFaceVertexIndices1(new int[faceCount]);
		def.setFaceVertexIndices2(new int[faceCount]);
		def.setFaceVertexIndices3(new int[faceCount]);
		if (textureCount > 0)
		{
			def.setTextureRenderTypes(new byte[textureCount]);
			def.setTextureTriangleVertexIndices1(new short[textureCount]);
			def.setTextureTriangleVertexIndices2(new short[textureCount]);
			def.setTextureTriangleVertexIndices3(new short[textureCount]);
		}

		if (hasVertexSkins == 1)
		{
			def.setVertexSkins(new int[vertexCount]);
		}

		if (isTextured == 1)
		{
			def.setFaceRenderTypes(new byte[faceCount]);
			def.setTextureCoordinates(new byte[faceCount]);
			def.setFaceTextures(new short[faceCount]);
		}

		if (faceRenderPriority == 255)
		{
			def.setFaceRenderPriorities(new byte[faceCount]);
		}
		else
		{
			def.setPriority((byte) faceRenderPriority);
		}

		if (hasFaceTransparencies == 1)
		{
			def.setFaceAlphas(new byte[faceCount]);
		}

		if (hasPackedTransparencyVertexGroups == 1)
		{
			def.setFaceSkins(new int[faceCount]);
		}

		def.setFaceColors(new short[faceCount]);
		stream1.position(offsetOfVertexFlags);
		stream2.position(offsetOfVertexXData);
		stream3.position(offsetOfVertexYData);
		stream4.position(offsetOfVertexZData);
		stream5.position(offsetOfVertexSkins);
		int previousVertexX = 0;
		int previousVertexY = 0;
		int previousVertexZ = 0;

		for (int i = 0; i < vertexCount; ++i)
		{
			int vertexFlags = ByteBufferExtKt.readUnsignedByte(stream1);
			int deltaX = 0;
			if ((vertexFlags & 1) != 0)
			{
				deltaX = ByteBufferExtKt.readShortSmart(stream2);
			}

			int deltaY = 0;
			if ((vertexFlags & 2) != 0)
			{
				deltaY = ByteBufferExtKt.readShortSmart(stream3);
			}

			int deltaZ = 0;
			if ((vertexFlags & 4) != 0)
			{
				deltaZ = ByteBufferExtKt.readShortSmart(stream4);
			}

			def.getVertexPositionsX()[i] = previousVertexX + deltaX;
			def.getVertexPositionsY()[i] = previousVertexY + deltaY;
			def.getVertexPositionsZ()[i] = previousVertexZ + deltaZ;
			previousVertexX = def.getVertexPositionsX()[i];
			previousVertexY = def.getVertexPositionsY()[i];
			previousVertexZ = def.getVertexPositionsZ()[i];
			if (hasVertexSkins == 1)
			{
				def.getVertexSkins()[i] = ByteBufferExtKt.readUnsignedByte(stream5);
			}
		}

		stream1.position(offsetOfFaceColorsOrFaceTextures);
		stream2.position(offsetOfFaceTextureFlags);
		stream3.position(offsetOfFaceRenderPriorities);
		stream4.position(offsetOfFaceTransparencies);
		stream5.position(offsetOfPackedTransparencyVertexGroups);

		for (int i = 0; i < faceCount; ++i)
		{
			def.getFaceColors()[i] = (short) ByteBufferExtKt.readUnsignedShort(stream1);
			if (isTextured == 1)
			{
				int faceTextureFlags = ByteBufferExtKt.readUnsignedByte(stream2);
				if ((faceTextureFlags & 1) == 1)
				{
					def.getFaceRenderTypes()[i] = 1;
					usesFaceRenderTypes = true;
				}
				else
				{
					def.getFaceRenderTypes()[i] = 0;
				}

				if ((faceTextureFlags & 2) == 2)
				{
					def.getTextureCoordinates()[i] = (byte) (faceTextureFlags >> 2);
					def.getFaceTextures()[i] = def.getFaceColors()[i];
					def.getFaceColors()[i] = 127;
					if (def.getFaceTextures()[i] != -1)
					{
						usesFaceTextures = true;
					}
				}
				else
				{
					def.getTextureCoordinates()[i] = -1;
					def.getFaceTextures()[i] = -1;
				}
			}

			if (faceRenderPriority == 255)
			{
				def.getFaceRenderPriorities()[i] = stream3.get();
			}

			if (hasFaceTransparencies == 1)
			{
				def.getFaceAlphas()[i] = stream4.get();
			}

			if (hasPackedTransparencyVertexGroups == 1)
			{
				def.getFaceSkins()[i] = ByteBufferExtKt.readUnsignedByte(stream5);
			}
		}

		stream1.position(offsetOfFaceIndexData);
		stream2.position(offsetOfFaceIndexCompressionTypes);
		int previousIndex1 = 0;
		int previousIndex2 = 0;
		int previousIndex3 = 0;

		for (int i = 0; i < faceCount; ++i)
		{
			int faceIndexCompressionType = ByteBufferExtKt.readUnsignedByte(stream2);
			if (faceIndexCompressionType == 1)
			{
				previousIndex1 = ByteBufferExtKt.readShortSmart(stream1) + previousIndex3;
				previousIndex2 = ByteBufferExtKt.readShortSmart(stream1) + previousIndex1;
				previousIndex3 = ByteBufferExtKt.readShortSmart(stream1) + previousIndex2;
				def.getFaceVertexIndices1()[i] = previousIndex1;
				def.getFaceVertexIndices2()[i] = previousIndex2;
				def.getFaceVertexIndices3()[i] = previousIndex3;
			}

			if (faceIndexCompressionType == 2)
			{
				previousIndex2 = previousIndex3;
				previousIndex3 = ByteBufferExtKt.readShortSmart(stream1) + previousIndex3;
				def.getFaceVertexIndices1()[i] = previousIndex1;
				def.getFaceVertexIndices2()[i] = previousIndex2;
				def.getFaceVertexIndices3()[i] = previousIndex3;
			}

			if (faceIndexCompressionType == 3)
			{
				previousIndex1 = previousIndex3;
				previousIndex3 = ByteBufferExtKt.readShortSmart(stream1) + previousIndex3;
				def.getFaceVertexIndices1()[i] = previousIndex1;
				def.getFaceVertexIndices2()[i] = previousIndex2;
				def.getFaceVertexIndices3()[i] = previousIndex3;
			}

			if (faceIndexCompressionType == 4)
			{
				int swap = previousIndex1;
				previousIndex1 = previousIndex2;
				previousIndex2 = swap;
				previousIndex3 = ByteBufferExtKt.readShortSmart(stream1) + previousIndex3;
				def.getFaceVertexIndices1()[i] = previousIndex1;
				def.getFaceVertexIndices2()[i] = previousIndex2;
				def.getFaceVertexIndices3()[i] = previousIndex3;
			}
		}

		stream1.position(offsetOfTextureIndices);

		for (int i = 0; i < textureCount; ++i)
		{
			def.getTextureRenderTypes()[i] = 0;
			def.getTextureTriangleVertexIndices1()[i] = (short) ByteBufferExtKt.readUnsignedShort(stream1);
			def.getTextureTriangleVertexIndices2()[i] = (short) ByteBufferExtKt.readUnsignedShort(stream1);
			def.getTextureTriangleVertexIndices3()[i] = (short) ByteBufferExtKt.readUnsignedShort(stream1);
		}

		if (def.getTextureCoordinates() != null)
		{
			boolean usesTextureCoords = false;

			for (int i = 0; i < faceCount; ++i)
			{
				int coord = def.getTextureCoordinates()[i] & 255;
				if (coord != 255)
				{
					if (def.getFaceVertexIndices1()[i] == (def.getTextureTriangleVertexIndices1()[coord] & '\uffff') && def.getFaceVertexIndices2()[i] == (def.getTextureTriangleVertexIndices2()[coord] & '\uffff') && def.getFaceVertexIndices3()[i] == (def.getTextureTriangleVertexIndices3()[coord] & '\uffff'))
					{
						def.getTextureCoordinates()[i] = -1;
					}
					else
					{
						usesTextureCoords = true;
					}
				}
			}

			if (!usesTextureCoords)
			{
				def.setTextureCoordinates(null);
			}
		}

		if (!usesFaceTextures)
		{
			def.setFaceTextures(null);
		}

		if (!usesFaceRenderTypes)
		{
			def.setFaceRenderTypes(null);
		}

	}

}
