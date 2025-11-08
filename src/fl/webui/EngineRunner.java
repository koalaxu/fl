package fl.webui;

import java.util.concurrent.locks.Lock;

import fl.engine.Engine;
import fl.engine.data.DataAccessor;

public class EngineRunner extends Thread {
	public EngineRunner(Engine engine, Lock lock) {
		this.engine = engine;
		this.lock = lock;
	}

	@Override
	public void run() {
		lock.lock();
		try {
			engine.Proceed();
			engine.Done();
			engine.Update();
			DataAccessor.SetProgress(1);
			running = false;
		} finally {
			lock.unlock();
		}
	}
	
	private Engine engine;
	private Lock lock;
	
	public static boolean running = false;
}
