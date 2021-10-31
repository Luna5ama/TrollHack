package cum.xiaro.trollhack.util.threads

import cum.xiaro.trollhack.TrollHackMod
import kotlinx.coroutines.*

@Suppress("EXPERIMENTAL_API_USAGE")
internal object BackgroundScope : CoroutineScope by CoroutineScope(newFixedThreadPoolContext(2, "Troll Hack Background")) {

    private val jobs = LinkedHashMap<BackgroundJob, Job?>()
    private var started = false

    fun start() {
        started = true
        for ((job, _) in jobs) {
            jobs[job] = startJob(job)
        }
    }

    fun launchLooping(name: String, delay: Long, block: suspend CoroutineScope.() -> Unit): BackgroundJob {
        return launchLooping(BackgroundJob(name, delay, block))
    }

    fun launchLooping(job: BackgroundJob): BackgroundJob {
        if (!started) {
            jobs[job] = null
        } else {
            jobs[job] = startJob(job)
        }

        return job
    }

    fun cancel(job: BackgroundJob) = jobs.remove(job)?.cancel()

    private fun startJob(job: BackgroundJob): Job {
        return launch {
            while (isActive) {
                try {
                    job.block(this)
                } catch (e: Exception) {
                    TrollHackMod.logger.warn("Error occurred while running background job ${job.name}", e)
                }
                delay(job.delay)
            }
        }
    }

}
