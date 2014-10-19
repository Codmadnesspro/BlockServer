package org.blockserver.level.provider.bsl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.blockserver.Server;
import org.blockserver.io.bsf.BSF;
import org.blockserver.io.bsf.BSFReader;
import org.blockserver.level.provider.ChunkPosition;
import org.blockserver.level.provider.LevelCorruptedException;
import org.blockserver.level.provider.LevelProvider;
import org.blockserver.math.Vector3d;

public class BSLLevelProvider extends LevelProvider{
	private Server server;
	private File dir, chunksDir;
	private Vector3d spawnPos;
	private Map<ChunkPosition, BSLChunk> cachedChunks = new HashMap<ChunkPosition, BSLChunk>();

	public BSLLevelProvider(Server server, File file, String name){
		super(name);
		this.server = server;
		dir = file;
		chunksDir = new File(file, "chunks");
		chunksDir.mkdirs();
	}

	@Override
	public void init() throws LevelCorruptedException{
		File index = new File(dir, "index.bsf");
		if(dir.isDirectory() && index.isFile()){
			try{
				BSFReader reader = new BSFReader(new FileInputStream(index));
				try{
					Map<String, Object> data = reader.readAll();
					reader.close();
					if(!reader.getType().equals(BSF.Type.LEVEL_INDEX)){
						throw new BSF.InvalidBSFFileException("BSF level index corrupted: incorrect type!");
					}
					spawnPos = new Vector3d(
							(double) data.get(BSF.LI_SPAWN_X),
							(double) data.get(BSF.LI_SPAWN_Y),
							(double) data.get(BSF.LI_SPAWN_Z));
				}
				catch(BSF.InvalidBSFFileException | NullPointerException e){
					try{
						reader.close();
					}
					catch(Exception ex){}
					throw new LevelCorruptedException(e, this);
				}
			}
			catch(IOException e){
				e.printStackTrace();
			}
		}
		else{
			if(!dir.mkdirs()){
				throw new RuntimeException("Unable to make world directories");
			}
			// TODO level generation
		}
	}

	@Override
	public boolean loadChunk(ChunkPosition pos){
		if(cachedChunks.containsKey(pos)){
			return false;
		}
		try{
			cachedChunks.put(pos, new BSLChunk(server, toFile(pos), pos));
			return true;
		}
		catch(IOException e){
			e.printStackTrace();
			return false;
		}
	}
	@Override
	public boolean saveChunk(ChunkPosition pos){
		if( !cachedChunks.containsKey(pos) ) return false;
		try{
			cachedChunks.get(pos).save();
		}
		catch(IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	@Override
	public boolean isChunkLoaded(ChunkPosition pos){
		return cachedChunks.containsKey(pos);
	}
	@Override
	public boolean isChunkGenerated(ChunkPosition pos){
		return toFile(pos).exists();
	}
	@Override
	public boolean deleteChunk(ChunkPosition pos){
		toFile(pos).delete();
		return true;
	}

	@Override
	public Vector3d getSpawn(){
		return spawnPos;
	}

	@Override
	public Server getServer(){
		return server;
	}

	public File toFile(ChunkPosition pos){
		return new File(chunksDir, String.format("%d_%d.bsc", pos.getX(), pos.getZ()));
	}

	@Override
	public BSLChunk getChunk(ChunkPosition pos){
		return cachedChunks.get(pos);
	}
}
