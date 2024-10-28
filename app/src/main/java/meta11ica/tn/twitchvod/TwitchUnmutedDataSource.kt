package meta11ica.tn.twitchvod
import java.io.IOException
import android.net.Uri
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.TransferListener

@UnstableApi
class TwitchUnmutedDataSourceFactory(
    private val defaultDataSourceFactory: DataSource.Factory
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return TwitchUnmutedDataSource(defaultDataSourceFactory.createDataSource())
    }
}
@UnstableApi
class TwitchUnmutedDataSource(
    private val dataSource: DataSource
) : DataSource {

    private lateinit var currentUri: Uri

    private fun tryOpeningSegment(dataSpec: DataSpec): Long {
        return try {
            // Try to open with current URI
            dataSource.close()
            dataSource.open(dataSpec.withUri(currentUri))
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            // Check if the error is 403 Forbidden
            if (e.responseCode == 403) {
                if (currentUri.toString().contains("-unmuted.ts")) {
                    // Try with the fallback URL (replace -unmuted.ts with -muted.ts)
                    currentUri = Uri.parse(currentUri.toString().replace("-unmuted.ts", "-muted.ts"))
                    return tryOpeningSegment(dataSpec) // Retry with modified URL
                } else {
                    // Skip this segment if fallback also fails
                    throw DataSourceException(DataSourceException.POSITION_OUT_OF_RANGE)
                }
            } else {
                throw e // Throw other errors to ExoPlayer
            }
        }
    }

    override fun open(dataSpec: DataSpec): Long {
        currentUri = dataSpec.uri
        return tryOpeningSegment(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        return dataSource.read(buffer, offset, readLength)
    }

    override fun addTransferListener(transferListener: TransferListener) {
    }

    override fun getUri(): Uri? {
        return currentUri ?: dataSource.uri
    }

    override fun close() {
        dataSource.close()
    }
}
