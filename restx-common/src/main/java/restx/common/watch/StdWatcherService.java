package restx.common.watch;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.eventbus.EventBus;

import restx.common.watch.exceptions.WatcherServiceException;

/**
* User: xavierhanin
* Date: 7/27/13
* Time: 2:17 PM
*/
public class StdWatcherService implements WatcherService {
	
	private static final Logger LOGGER = Logger.getLogger(StdWatcherService.class.getName());
	
    @Override
    public Closeable watch(EventBus eventBus, ExecutorService executor, Path dir, WatcherSettings settings) {
        try {
            final WatchDir watchDir = new WatchDir(eventBus, dir, settings);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    watchDir.processEvents();
                }
            });
            return watchDir;
        } catch (IOException e) {
            throw new WatcherServiceException(e);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private static class WatchDir implements Closeable {
        private final WatchService watcher;
        private final Map<WatchKey,Path> keys;
        private final boolean recursive;
        private final EventCoalescor<FileWatchEvent> coalescor;
        private final Path root;
        private boolean trace = false;


        @SuppressWarnings("unchecked")
        static <T> WatchEvent<T> cast(WatchEvent<?> event) {
            return (WatchEvent<T>)event;
        }

        /**
         * Register the given directory with the WatchService
         */
        private void register(Path dir) throws IOException {
            WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            if (trace) {
                Path prev = keys.get(key);
                if (prev == null) {
                	LOGGER.log(Level.FINE, "register: {0}\n", dir);
                } else {
                    if (!dir.equals(prev)) {
                    	String prevPath = prev.toString();
                    	String dirPath = dir.toString();
                    	LOGGER.log(Level.FINE,"update: {0} -> {1}\n", new Object[] { prevPath, dirPath});
                    }
                }
            }
            keys.put(key, dir);
        }

        /**
         * Register the given directory, and all its sub-directories, with the
         * WatchService.
         */
        private void registerAll(final Path start) throws IOException {
            // register directory and sub-directories
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        /**
         * Creates a WatchService and registers the given directory
         */
        WatchDir(EventBus eventBus, Path dir, WatcherSettings settings) throws IOException {
            this.watcher = FileSystems.getDefault().newWatchService();
            this.keys = new HashMap<>();
            this.recursive = settings.recurse();
            this.root = dir;
            this.coalescor = FileWatchEventCoalescor.create(eventBus, settings.coalescePeriod());

            if (recursive) {
                registerAll(dir);
            } else {
                register(dir);
            }
        }



        /**
         * Process all events for keys queued to the watcher
         */
        void processEvents() {
            try {
                for (;;) {

					// wait for key to be signalled
					WatchKey key;
					try {
						key = watcher.take();
					} catch (InterruptedException x) {
						return;
					}

					Path dir = keys.get(key);
					if (dir == null) {
						LOGGER.log(Level.WARNING, "WatchKey not recognized!!");
						continue;
					}

					for (WatchEvent<?> event: key.pollEvents()) {
						WatchEvent.Kind kind = event.kind();

						// Context for directory entry event is the file name of entry
						WatchEvent<Path> ev = cast(event);

						coalescor.post(FileWatchEvent.newInstance(root, dir, ev.context(), ev.kind(), ev.count()));

						if (kind == OVERFLOW) {
							continue;
						}

						Path name = ev.context();
						Path child = dir.resolve(name);

						// if directory is created, and watching recursively, then
						// register it and its sub-directories
						if (recursive && (kind == ENTRY_CREATE)) {
							deeplyRegister(child);
						}
					}

					// reset key and remove from set if directory no longer accessible
					boolean valid = key.reset();
					if (!valid) {
						keys.remove(key);

						// all directories are inaccessible
						if (keys.isEmpty()) {
							break;
						}
					}
				}
            } catch (ClosedWatchServiceException e) {
                // just ignore this exception, it just meant that the service has been closed,
                // while the current thread was waiting some event with the "take" method.
            }
        }

		private void deeplyRegister(Path child) {
			try {
				if (child.toFile().isDirectory()) {
					registerAll(child);
				}
			} catch (IOException x) {
				// ignore to keep sample readbale
			}
		}

        @Override
        public void close() throws IOException {
            watcher.close();
            coalescor.close();
        }
    }
}
