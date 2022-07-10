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
	private static final byte HAS_DELTA_X = 1;
	private static final byte HAS_DELTA_Y = 2;
	private static final byte HAS_DELTA_Z = 4;

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

	private void decodeType3(ModelDefinition def, byte[] inputData) {
		ByteBuffer stream1 = ByteBuffer.wrap(inputData);
		ByteBuffer stream2 = ByteBuffer.wrap(inputData);
		ByteBuffer stream3 = ByteBuffer.wrap(inputData);
		ByteBuffer stream4 = ByteBuffer.wrap(inputData);
		ByteBuffer stream5 = ByteBuffer.wrap(inputData);
		ByteBuffer stream6 = ByteBuffer.wrap(inputData);
		ByteBuffer stream7 = ByteBuffer.wrap(inputData);
		stream1.position(inputData.length - 26);
		int vertexCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int faceCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int textureCount = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasFaceRenderTypes = ByteBufferExtKt.readUnsignedByte(stream1);
		int faceRenderPriority = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasFaceTransparencies = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasPackedTransparencyVertexGroups = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasFaceTextures = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasVertexSkins = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasAnimayaGroups = ByteBufferExtKt.readUnsignedByte(stream1);
		int var19 = ByteBufferExtKt.readUnsignedShort(stream1);
		int var20 = ByteBufferExtKt.readUnsignedShort(stream1);
		int var21 = ByteBufferExtKt.readUnsignedShort(stream1);
		int var22 = ByteBufferExtKt.readUnsignedShort(stream1);
		int var23 = ByteBufferExtKt.readUnsignedShort(stream1);
		int var24 = ByteBufferExtKt.readUnsignedShort(stream1);
		int renderTypeZeroCount = 0;
		int renderTypeOtherCount = 0;
		int renderTypeTwoCount = 0;
		stream1.rewind();
		if (textureCount > 0)
		{
			final byte[] textureRenderTypes = new byte[textureCount];
			stream1.get(textureRenderTypes);

			for (byte renderType : textureRenderTypes)
			{
				if (renderType == 0)
				{
					++renderTypeZeroCount;
				}

				if (renderType >= 1 && renderType <= 3)
				{
					++renderTypeOtherCount;
				}

				if (renderType == 2)
				{
					++renderTypeTwoCount;
				}
			}

			def.setTextureRenderTypes(textureRenderTypes);
		}

		int dataOffset = textureCount;
		int offsetOfVertexFlags = dataOffset;
		dataOffset += vertexCount;
		int offsetOfFaceRenderTypes = dataOffset;
		if (hasFaceRenderTypes == 1)
		{
			dataOffset += faceCount;
		}

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

		int offsetOfVertexSkins = dataOffset;
		dataOffset += var24;
		int offsetOfFaceTransparencies = dataOffset;
		if (hasFaceTransparencies == 1)
		{
			dataOffset += faceCount;
		}

		int offsetOfFaceIndexData = dataOffset;
		dataOffset += var22;
		int offsetOfFaceTextures = dataOffset;
		if (hasFaceTextures == 1)
		{
			dataOffset += faceCount * 2;
		}

		int offsetOfTextureCoordinates = dataOffset;
		dataOffset += var23;
		int offsetOfFaceColors = dataOffset;
		dataOffset += faceCount * 2;
		int offsetOfVertexXData = dataOffset;
		dataOffset += var19;
		int offsetOfVertexYData = dataOffset;
		dataOffset += var20;
		int offsetOfVertexZData = dataOffset;
		dataOffset += var21;
		int var42 = dataOffset;
		dataOffset += renderTypeZeroCount * 6;
		int var43 = dataOffset;
		dataOffset += renderTypeOtherCount * 6;
		int var44 = dataOffset;
		dataOffset += renderTypeOtherCount * 6;
		int var45 = dataOffset;
		dataOffset += renderTypeOtherCount * 2;
		int var46 = dataOffset;
		dataOffset += renderTypeOtherCount;
		int var47 = dataOffset;
		dataOffset = dataOffset + renderTypeOtherCount * 2 + renderTypeTwoCount * 2;
		int offsetOfUnknown = dataOffset;
		def.setVertexCount(vertexCount);
		def.setFaceCount(faceCount);
		def.setTextureTriangleCount(textureCount);
		def.setVertexPositionsX(new int[vertexCount]);
		def.setVertexPositionsY(new int[vertexCount]);
		def.setVertexPositionsZ(new int[vertexCount]);
		def.setFaceVertexIndices1(new int[faceCount]);
		def.setFaceVertexIndices2(new int[faceCount]);
		def.setFaceVertexIndices3(new int[faceCount]);

		if (faceRenderPriority != 255) {
			def.setPriority((byte) faceRenderPriority);
		}

		if (hasPackedTransparencyVertexGroups == 1)
		{
			def.setFaceSkins(new int[faceCount]);
		}

		if (hasFaceTextures == 1)
		{
			def.setFaceTextures(new short[faceCount]);
		}

		if (hasFaceTextures == 1 && textureCount > 0)
		{
			def.setTextureCoordinates(new byte[faceCount]);
		}

		if (hasAnimayaGroups == 1)
		{
//			def.setAnimayaGroups(new int[vertexCount][]);
//			def.setAnimayaScales(new int[vertexCount][]);
		}

		if (textureCount > 0)
		{
			def.setTextureTriangleVertexIndices1(new short[textureCount]);
			def.setTextureTriangleVertexIndices2(new short[textureCount]);
			def.setTextureTriangleVertexIndices3(new short[textureCount]);
		}

		stream1.position(offsetOfVertexFlags);
		final byte[] vertexFlags = new byte[vertexCount];
		stream1.get(vertexFlags);

		stream1.position(offsetOfVertexXData);
		readVertexData(def, stream1, vertexFlags);

		if (hasVertexSkins == 1)
		{
			stream1.position(offsetOfVertexSkins);
			readVertexSkins(def, stream1, vertexCount);
		}

		if (hasAnimayaGroups == 1)
		{
			for (int i = 0; i < vertexCount; ++i)
			{
				int animayaLength = ByteBufferExtKt.readUnsignedByte(stream5);
//				def.getAnimayaGroups()[i] = new int[animayaLength];
//				def.getAnimayaScales()[i] = new int[animayaLength];

				for (int j = 0; j < animayaLength; ++j)
				{
					/*def.getAnimayaGroups()[i][j] =*/ ByteBufferExtKt.readUnsignedByte(stream5);
					/*def.getAnimayaScales()[i][j] =*/ ByteBufferExtKt.readUnsignedByte(stream5);
				}
			}
		}

		stream1.position(offsetOfFaceColors);
		readFaceColors(def, stream1, faceCount);

		if (hasFaceRenderTypes == 1) {
			stream1.position(offsetOfFaceRenderTypes);
			final byte[] faceRenderTypes = new byte[faceCount];
			stream1.get(faceRenderTypes);
			def.setFaceRenderTypes(faceRenderTypes);
		}

		if (faceRenderPriority == 255) {
			stream1.position(offsetOfFaceRenderPriorities);
			final byte[] faceRenderPriorities = new byte[faceCount];
			stream1.get(faceRenderPriorities);
			def.setFaceRenderPriorities(faceRenderPriorities);
		}

		if (hasFaceTransparencies == 1) {
			stream1.position(offsetOfFaceTransparencies);
			def.setFaceAlphas(readByteArray(stream1, faceCount));
		}

		stream5.position(offsetOfPackedTransparencyVertexGroups);
		stream6.position(offsetOfFaceTextures);
		stream7.position(offsetOfTextureCoordinates);

		for (int i = 0; i < faceCount; ++i)
		{
			if (hasPackedTransparencyVertexGroups == 1)
			{
				def.getFaceSkins()[i] = ByteBufferExtKt.readUnsignedByte(stream5);
			}

			if (hasFaceTextures == 1)
			{
				def.getFaceTextures()[i] = (short) (ByteBufferExtKt.readUnsignedShort(stream6) - 1);
			}

			if (def.getTextureCoordinates() != null && def.getFaceTextures()[i] != -1)
			{
				def.getTextureCoordinates()[i] = (byte) (ByteBufferExtKt.readUnsignedByte(stream7) - 1);
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

		stream1.position(var42);
		if (false) {
			// Removed due to lack of side effects outside the function
			stream2.position(var43);
			stream3.position(var44);
			stream4.position(var45);
			stream5.position(var46);
			stream6.position(var47);
		}

		for (int i = 0; i < textureCount; ++i)
		{
			int var56 = def.getTextureRenderTypes()[i] & 255;
			if (var56 == 0)
			{
				
				def.getTextureTriangleVertexIndices1()[i] = (short) ByteBufferExtKt.readUnsignedShort(stream1);
				def.getTextureTriangleVertexIndices2()[i] = (short) ByteBufferExtKt.readUnsignedShort(stream1);
				def.getTextureTriangleVertexIndices3()[i] = (short) ByteBufferExtKt.readUnsignedShort(stream1);
			}
		}

		if (false) {
			// Removed due to lack of side effects outside the function
			stream1.position(offsetOfUnknown);
			int unk = ByteBufferExtKt.readUnsignedByte(stream1);
			if (unk != 0)
			{
				ByteBufferExtKt.readUnsignedShort(stream1);
				ByteBufferExtKt.readUnsignedShort(stream1);
				ByteBufferExtKt.readUnsignedShort(stream1);
				ByteBufferExtKt.read24BitInt(stream1);
			}
		}
	}

	private void decodeType2(ModelDefinition def, byte[] inputData)
	{
		boolean usesFaceRenderTypes = false;
		boolean usesFaceTextures = false;
		ByteBuffer stream1 = ByteBuffer.wrap(inputData);
		ByteBuffer stream2 = ByteBuffer.wrap(inputData);
		ByteBuffer stream3 = ByteBuffer.wrap(inputData);
		ByteBuffer stream4 = ByteBuffer.wrap(inputData);
		ByteBuffer stream5 = ByteBuffer.wrap(inputData);
		stream1.position(inputData.length - 23);
		int vertexCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int faceCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int textureCount = ByteBufferExtKt.readUnsignedByte(stream1);
		int isTextured = ByteBufferExtKt.readUnsignedByte(stream1);
		int faceRenderPriority = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasFaceTransparencies = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasPackedTransparencyVertexGroups = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasVertexSkins = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasAnimayaGroups = ByteBufferExtKt.readUnsignedByte(stream1);
		int vertexXDataByteCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int vertexYDataByteCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int vertexZDataByteCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int faceIndexDataByteCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int vertexSkinsDataByteCount = ByteBufferExtKt.readUnsignedShort(stream1);
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
		dataOffset += vertexSkinsDataByteCount;
		int offsetOfFaceTransparencies = dataOffset;
		if (hasFaceTransparencies == 1)
		{
			dataOffset += faceCount;
		}

		int offsetOfFaceIndexData = dataOffset;
		dataOffset += faceIndexDataByteCount;
		int offsetOfFaceColors = dataOffset;
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

		if (isTextured == 1)
		{
			def.setFaceRenderTypes(new byte[faceCount]);
			def.setTextureCoordinates(new byte[faceCount]);
			def.setFaceTextures(new short[faceCount]);
		}

		if (faceRenderPriority != 255) {
			def.setPriority((byte) faceRenderPriority);
		}

		if (hasPackedTransparencyVertexGroups == 1)
		{
			def.setFaceSkins(new int[faceCount]);
		}

		if (hasAnimayaGroups == 1)
		{
//			def.setAnimayaGroups(new int[vertexCount][]);
//			def.setAnimayaScales(new int[vertexCount][]);
		}

		stream1.position(offsetOfVertexFlags);
		final byte[] vertexFlags = new byte[vertexCount];
		stream1.get(vertexFlags);

		stream1.position(offsetOfVertexXData);
		readVertexData(def, stream1, vertexFlags);

		if (hasVertexSkins == 1)
		{
			stream1.position(offsetOfVertexSkins);
			readVertexSkins(def, stream1, vertexCount);
		}

		if (hasAnimayaGroups == 1)
		{
			for (int i = 0; i < vertexCount; ++i)
			{
				int animayaLength = ByteBufferExtKt.readUnsignedByte(stream5);
//				def.getAnimayaGroups()[i] = new int[animayaLength];
//				def.getAnimayaScales()[i] = new int[animayaLength];

				for (int j = 0; j < animayaLength; ++j)
				{
					/*def.getAnimayaGroups()[i][j] =*/ ByteBufferExtKt.readUnsignedByte(stream5);
					/*def.getAnimayaScales()[i][j] =*/ ByteBufferExtKt.readUnsignedByte(stream5);
				}
			}
		}

		stream1.position(offsetOfFaceColors);
		readFaceColors(def, stream1, faceCount);

		stream2.position(offsetOfFaceTextureFlags);

		if (faceRenderPriority == 255) {
			stream1.position(offsetOfFaceRenderPriorities);
			final byte[] faceRenderPriorities = new byte[faceCount];
			stream1.get(faceRenderPriorities);
			def.setFaceRenderPriorities(faceRenderPriorities);
		}

		if (hasFaceTransparencies == 1) {
			stream1.position(offsetOfFaceTransparencies);
			def.setFaceAlphas(readByteArray(stream1, faceCount));
		}

		stream5.position(offsetOfPackedTransparencyVertexGroups);

		for (int i = 0; i < faceCount; ++i)
		{
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
			boolean useTextureCoords = false;

			for (int i = 0; i < faceCount; ++i)
			{
				int coord = def.getTextureCoordinates()[i] & 255;
				if (coord != 255)
				{
					if (def.getFaceVertexIndices1()[i] == (def.getTextureTriangleVertexIndices1()[coord] & 0xffff) && def.getFaceVertexIndices2()[i] == (def.getTextureTriangleVertexIndices2()[coord] & 0xffff) && def.getFaceVertexIndices3()[i] == (def.getTextureTriangleVertexIndices3()[coord] & 0xffff))
					{
						def.getTextureCoordinates()[i] = -1;
					}
					else
					{
						useTextureCoords = true;
					}
				}
			}

			if (!useTextureCoords)
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

	private void decodeType1(ModelDefinition def, byte[] inputData)
	{
		ByteBuffer stream1 = ByteBuffer.wrap(inputData);
		ByteBuffer stream2 = ByteBuffer.wrap(inputData);
		ByteBuffer stream3 = ByteBuffer.wrap(inputData);
		ByteBuffer stream4 = ByteBuffer.wrap(inputData);
		ByteBuffer stream5 = ByteBuffer.wrap(inputData);
		ByteBuffer stream6 = ByteBuffer.wrap(inputData);
		ByteBuffer stream7 = ByteBuffer.wrap(inputData);
		stream1.position(inputData.length - 23);
		int vertexCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int faceCount = ByteBufferExtKt.readUnsignedShort(stream1);
		int textureCount = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasFaceRenderTypes = ByteBufferExtKt.readUnsignedByte(stream1);
		int faceRenderPriority = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasFaceTransparencies = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasPackedTransparencyVertexGroups = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasFaceTextures = ByteBufferExtKt.readUnsignedByte(stream1);
		int hasVertexSkins = ByteBufferExtKt.readUnsignedByte(stream1);
		int var18 = ByteBufferExtKt.readUnsignedShort(stream1);
		int var19 = ByteBufferExtKt.readUnsignedShort(stream1);
		int var20 = ByteBufferExtKt.readUnsignedShort(stream1);
		int var21 = ByteBufferExtKt.readUnsignedShort(stream1);
		int var22 = ByteBufferExtKt.readUnsignedShort(stream1);
		int renderTypeZeroCount = 0;
		int renderTypeOtherCount = 0;
		int renderTypeTwoCount = 0;
		stream1.rewind();
		if (textureCount > 0)
		{
			final byte[] textureRenderTypes = new byte[textureCount];
			stream1.get(textureRenderTypes);

			for (byte renderType : textureRenderTypes)
			{
				if (renderType == 0)
				{
					++renderTypeZeroCount;
				}

				if (renderType >= 1 && renderType <= 3)
				{
					++renderTypeOtherCount;
				}

				if (renderType == 2)
				{
					++renderTypeTwoCount;
				}
			}

			def.setTextureRenderTypes(textureRenderTypes);
		}
		int dataOffset = textureCount;
		int offsetOfVertexFlags = dataOffset;
		dataOffset += vertexCount;
		int offsetOfFaceRenderTypes = dataOffset;
		if (hasFaceRenderTypes == 1)
		{
			dataOffset += faceCount;
		}

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
		dataOffset += var21;
		int offsetOfFaceTextures = dataOffset;
		if (hasFaceTextures == 1)
		{
			dataOffset += faceCount * 2;
		}

		int offsetOfTextureCoordinates = dataOffset;
		dataOffset += var22;
		int offsetOfFaceColors = dataOffset;
		dataOffset += faceCount * 2;
		int offsetOfVertexXData = dataOffset;
		dataOffset += var18;
		int offsetOfVertexYData = dataOffset;
		dataOffset += var19;
		int offsetOfVertexZData = dataOffset;
		dataOffset += var20;
		int offsetOfTextureIndices = dataOffset;
		dataOffset += renderTypeZeroCount * 6;
		int var41 = dataOffset;
		dataOffset += renderTypeOtherCount * 6;
		int var42 = dataOffset;
		dataOffset += renderTypeOtherCount * 6;
		int var43 = dataOffset;
		dataOffset += renderTypeOtherCount * 2;
		int var44 = dataOffset;
		dataOffset += renderTypeOtherCount;
		int var45 = dataOffset;
		dataOffset = dataOffset + renderTypeOtherCount * 2 + renderTypeTwoCount * 2;
		int offsetOfUnknown = dataOffset;
		def.setVertexCount(vertexCount);
		def.setFaceCount(faceCount);
		def.setTextureTriangleCount(textureCount);
		def.setVertexPositionsX(new int[vertexCount]);
		def.setVertexPositionsY(new int[vertexCount]);
		def.setVertexPositionsZ(new int[vertexCount]);
		def.setFaceVertexIndices1(new int[faceCount]);
		def.setFaceVertexIndices2(new int[faceCount]);
		def.setFaceVertexIndices3(new int[faceCount]);

		if (faceRenderPriority != 255) {
			def.setPriority((byte) faceRenderPriority);
		}

		if (hasPackedTransparencyVertexGroups == 1)
		{
			def.setFaceSkins(new int[faceCount]);
		}

		if (hasFaceTextures == 1)
		{
			def.setFaceTextures(new short[faceCount]);
		}

		if (hasFaceTextures == 1 && textureCount > 0)
		{
			def.setTextureCoordinates(new byte[faceCount]);
		}

		if (textureCount > 0)
		{
			def.setTextureTriangleVertexIndices1(new short[textureCount]);
			def.setTextureTriangleVertexIndices2(new short[textureCount]);
			def.setTextureTriangleVertexIndices3(new short[textureCount]);
		}

		stream1.position(offsetOfVertexFlags);
		final byte[] vertexFlags = new byte[vertexCount];
		stream1.get(vertexFlags);

		stream1.position(offsetOfVertexXData);
		readVertexData(def, stream1, vertexFlags);

		if (hasVertexSkins == 1)
		{
			stream1.position(offsetOfVertexSkins);
			readVertexSkins(def, stream1, vertexCount);
		}


		stream1.position(offsetOfFaceColors);
		readFaceColors(def, stream1, faceCount);

		if (hasFaceRenderTypes == 1) {
			stream1.position(offsetOfFaceRenderTypes);
			def.setFaceRenderTypes(readByteArray(stream1, faceCount));
		}

		if (faceRenderPriority == 255) {
			stream1.position(offsetOfFaceRenderPriorities);
			def.setFaceRenderPriorities(readByteArray(stream1, faceCount));
		}

		if (hasFaceTransparencies == 1) {
			stream1.position(offsetOfFaceTransparencies);
			def.setFaceAlphas(readByteArray(stream1, faceCount));
		}

		stream5.position(offsetOfPackedTransparencyVertexGroups);
		stream6.position(offsetOfFaceTextures);
		stream7.position(offsetOfTextureCoordinates);

		for (int i = 0; i < faceCount; ++i)
		{
			if (hasPackedTransparencyVertexGroups == 1)
			{
				def.getFaceSkins()[i] = ByteBufferExtKt.readUnsignedByte(stream5);
			}

			if (hasFaceTextures == 1)
			{
				def.getFaceTextures()[i] = (short) (ByteBufferExtKt.readUnsignedShort(stream6) - 1);
			}

			if (def.getTextureCoordinates() != null && def.getFaceTextures()[i] != -1)
			{
				def.getTextureCoordinates()[i] = (byte) (ByteBufferExtKt.readUnsignedByte(stream7) - 1);
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
		if (false) {
			// Removed due to lack of side effects outside the function
			stream2.position(var41);
			stream3.position(var42);
			stream4.position(var43);
			stream5.position(var44);
			stream6.position(var45);
		}

		for (int i = 0; i < textureCount; ++i)
		{
			int textureRenderType = def.getTextureRenderTypes()[i] & 255;
			if (textureRenderType == 0)
			{
				def.getTextureTriangleVertexIndices1()[i] = (short) ByteBufferExtKt.readUnsignedShort(stream1);
				def.getTextureTriangleVertexIndices2()[i] = (short) ByteBufferExtKt.readUnsignedShort(stream1);
				def.getTextureTriangleVertexIndices3()[i] = (short) ByteBufferExtKt.readUnsignedShort(stream1);
			}
		}

		if (false) {
			// Removed due to lack of side effects outside the function
			stream1.position(offsetOfUnknown);
			int unk = ByteBufferExtKt.readUnsignedByte(stream1);
			if (unk != 0) {
				ByteBufferExtKt.readUnsignedShort(stream1);
				ByteBufferExtKt.readUnsignedShort(stream1);
				ByteBufferExtKt.readUnsignedShort(stream1);
				ByteBufferExtKt.read24BitInt(stream1);
			}
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
		int offsetOfFaceColors = dataOffset;
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

		if (isTextured == 1)
		{
			def.setFaceRenderTypes(new byte[faceCount]);
			def.setTextureCoordinates(new byte[faceCount]);
			def.setFaceTextures(new short[faceCount]);
		}

		if (faceRenderPriority != 255) {
			def.setPriority((byte) faceRenderPriority);
		}

		if (hasPackedTransparencyVertexGroups == 1)
		{
			def.setFaceSkins(new int[faceCount]);
		}

		stream1.position(offsetOfVertexFlags);
		final byte[] vertexFlags = readByteArray(stream1, vertexCount);

		stream1.position(offsetOfVertexXData);
		readVertexData(def, stream1, vertexFlags);

		if (hasVertexSkins == 1)
		{
			stream1.position(offsetOfVertexSkins);
			readVertexSkins(def, stream1, vertexCount);
		}

		stream1.position(offsetOfFaceColors);
		readFaceColors(def, stream1, faceCount);

		if (faceRenderPriority == 255) {
			stream1.position(offsetOfFaceRenderPriorities);
			def.setFaceRenderPriorities(readByteArray(stream1, faceCount));
		}

		stream2.position(offsetOfFaceTextureFlags);

		if (hasFaceTransparencies == 1) {
			stream1.position(offsetOfFaceTransparencies);
			def.setFaceAlphas(readByteArray(stream1, faceCount));
		}

		stream5.position(offsetOfPackedTransparencyVertexGroups);

		for (int i = 0; i < faceCount; ++i)
		{
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

	private void readVertexData(ModelDefinition def, ByteBuffer stream, byte[] vertexFlags) {
		def.setVertexPositionsX(readVertexGroup(stream, vertexFlags, HAS_DELTA_X));
		def.setVertexPositionsY(readVertexGroup(stream, vertexFlags, HAS_DELTA_Y));
		def.setVertexPositionsZ(readVertexGroup(stream, vertexFlags, HAS_DELTA_Z));
	}

	private void readFaceColors(ModelDefinition def, ByteBuffer stream, int faceCount) {
		final short[] faceColors = new short[faceCount];
		stream.asShortBuffer().get(faceColors);
		def.setFaceColors(faceColors);
	}

	private void readVertexSkins(ModelDefinition def, ByteBuffer stream, int vertexCount) {
		final int[] vertexSkins = new int[vertexCount];
		for (int i = 0; i < vertexCount; ++i) {
			vertexSkins[i] = ByteBufferExtKt.readUnsignedByte(stream);
		}
		def.setVertexSkins(vertexSkins);
	}

	private int[] readVertexGroup(ByteBuffer stream, byte[] vertexFlags, byte deltaMask) {
		int position = 0;
		final int vertexCount = vertexFlags.length;
		final int[] vertices = new int[vertexCount];
		for (int i = 0; i < vertexCount; ++i) {
			if ((vertexFlags[i] & deltaMask) != 0) {
				position += ByteBufferExtKt.readShortSmart(stream);
			}
			vertices[i] = position;
		}
		return vertices;
	}

	private byte[] readByteArray(ByteBuffer stream, int length) {
		final byte[] array = new byte[length];
		stream.get(array);
		return array;
	}
}
