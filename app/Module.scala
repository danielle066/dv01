import java.time.Clock

import com.google.inject.AbstractModule

/**
 * Guice module loaded automatically from the app root package.
 */
class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
  }
}
