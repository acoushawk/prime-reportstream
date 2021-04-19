package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import com.google.errorprone.annotations.CompatibleWith
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.WorkflowEngine.Header;
import gov.cdc.prime.router.azure.db.enums.TaskAction
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Base64
import org.json.JSONObject
import java.util.UUID
import java.util.logging.Level
import fuzzycsv.FuzzyCSVTable
import java.io.StringReader
import fuzzycsv.FuzzyStaticApi.count
 
class Facility private constructor(
    val organization: String?,
    val facility: String?,
    val CLIA: String?,
    val positive: Long?,
    val total: Long? ){
    
    data class Builder(
        var organization: String? = null,
        var facility: String? = null,
        var CLIA: String? = null,
        var positive: Long? = null,
        var total: Long? = null ){
        
        fun organization( organization: String ) = apply { this.organization = organization }
        fun facility( facility: String ) = apply { this.facility = facility }
        fun CLIA( CLIA: String ) = apply { this.CLIA = CLIA }
        fun positive( positive: Long ) = apply { this.positive = positive }
        fun total( total: Long ) = apply { this.total = total }
        fun build() = Facility( organization, facility, CLIA, positive, total )
    }
}

class Action private constructor(
    val date: Int?,
    val user: String?,
    val action: String? ){
    
    data class Builder(
        var date: Int? = null,
        var user: String? = null,
        var action: String? = null ){

        fun date( date: Int ) = apply { this.date = date }
        fun user( user: String ) = apply { this.user = user }
        fun action( action: String ) = apply { this.action = action }
        fun build() = Action( date, user, action )
    }
}

class ReportView private constructor( 
    val sent: String?,
    val via: String?,
    val positive: Long?,
    val total: Long?,
    val fileType: String?,
    val type: String?,
    val reportId: String?,
    val expires: Long?,
    val sendingOrg: String?,
    val receivingOrg: String?,
    val facilities: ArrayList<Facility>?,
    val actions: ArrayList<Action>? ){
    
    data class Builder(
        var sent: String? = null,
        var via: String? = null,
        var positive: Long? = null,
        var total: Long? = null, 
        var fileType: String? = null,
        var type: String? = null,
        var reportId: String? = null, 
        var expires: Long? = null,
        var sendingOrg: String? = null,
        var receivingOrg: String? = null,
        var facilities: ArrayList<Facility>? = ArrayList<Facility>(),
        var actions: ArrayList<Action>? = ArrayList<Action>() ){

        fun sent( sent: String ) = apply { this.sent = sent }
        fun via( via: String ) = apply { this.via = via }
        fun positive( positive: Long ) = apply { this.positive = positive }
        fun total( total: Long ) = apply{ this.total = total }
        fun fileType( fileType: String ) = apply { this.fileType = fileType }
        fun type( type: String ) = apply { this.type = type }
        fun reportId( reportId: String ) = apply { this.reportId = reportId }
        fun expires( expires: Long ) = apply { this.expires = expires }
        fun sendingOrg( sendingOrg: String ) = apply { this.sendingOrg = sendingOrg }
        fun receivingOrg( receivingOrg: String ) = apply { this.receivingOrg = receivingOrg }
        fun facilities( facilities: ArrayList<Facility> ) = apply { this.facilities = facilities }
        fun actions( actions: ArrayList<Action> ) = apply { this.actions = actions }
        fun build() = ReportView( sent, via, positive, total, fileType, type, reportId, expires, sendingOrg, receivingOrg, facilities, actions )
    }

}

class CardView private constructor(
    val id: String?,
    val title: String?,
    val subtitle: String?,
    val daily: Long?,
    val last: Long?,
    val positive: Boolean?,
    val change: Long?,
    val pct_change: Double?,
    val data: Array<Long>? ){
    
    data class Builder(
        var id: String? = null,
        var title: String? = null,
        var subtitle: String? = null,
        var daily: Long? = null,
        var last: Long? = null,
        var positive: Boolean? = null,
        var change: Long? = null,
        var pct_change: Double? = null,
        var data: Array<Long>? = emptyArray<Long>()){

        fun id( id: String ) = apply { this.id = id }
        fun title( title: String ) = apply {this.title = title}
        fun subtitle( subtitle: String ) = apply {this.subtitle = subtitle}
        fun daily( daily: Long ) = apply {this.daily = daily}
        fun last( last: Long ) = apply {this.last = last}
        fun positive( positive: Boolean ) = apply { this.positive = positive}
        fun change( change: Long ) = apply {this.change = change}
        fun pct_change( pct_change: Double ) = apply {this.pct_change = pct_change}
        fun data( data: Array<Long>) = apply {this.data = data}
        fun build() = CardView( id, title, subtitle, daily, last, positive, change, pct_change, data )
    }
}

class GetReports :
    BaseHistoryFunction() {

    @FunctionName("getReports")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getReports",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/report"
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        return GetReports(request, context)
    }
}

class GetReportById :
    BaseHistoryFunction() {
    @FunctionName("getReportById")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getReportById",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/report/{reportId}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("reportId") reportId: String,
        context: ExecutionContext,
    ): HttpResponseMessage {        
        return GetReportById(request, reportId, context)
    }
}

class GetSummaryTests: 
    BaseHistoryFunction() {
    
    @FunctionName("getSummaryTests")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getSummaryTest",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/summary/tests"
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext
    ): HttpResponseMessage {
        return GetSummaryTests( request, context); 
    }
}

class GetSummary: BaseHistoryFunction() {
    @FunctionName("getSummary")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getSummary",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/summary/field/{field}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("field") field: String,   
        context: ExecutionContext
    ): HttpResponseMessage {
        return request.createResponseBuilder(HttpStatus.NOT_IMPLEMENTED).build()
  
    }
}

open class BaseHistoryFunction {

    val DAYS_TO_SHOW = 7L
    val workflowEngine = WorkflowEngine()

    fun GetReports(request: HttpRequestMessage<String?>, context: ExecutionContext): HttpResponseMessage {
        val authClaims = checkAuthenticated(request, context)
        if( authClaims == null ) return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()
        var response: HttpResponseMessage;
        try{
            val headers = workflowEngine.db.fetchDownloadableReportFiles(
                OffsetDateTime.now().minusDays(DAYS_TO_SHOW), authClaims.organization.name
            )

            @Suppress( "NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER" )
            var reports = headers.sortedByDescending{ it.createdAt }.map {

                var facilities = arrayListOf<Facility>();
                if( it.bodyFormat == "CSV")
                    facilities = getFieldSummaryForReportId("Ordering_facility_name",it.reportId.toString(), authClaims)

                ReportView.Builder()
                .reportId( it.reportId.toString() )
                .sent( it.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) )
                .via( it.bodyFormat )
                .total( it.itemCount.toLong() )
                .fileType( it.bodyFormat )
                .type( "Electronic Laboratory Results" )
                .expires( DAYS_TO_SHOW - it.createdAt.until(OffsetDateTime.now(), ChronoUnit.DAYS), )
                .facilities(facilities)
                .build()        
            }

            response = request.createResponseBuilder(HttpStatus.OK)
                .body( reports )
                .header("Content-Type", "application/json")
                .build()
        }catch (ex: Exception) {
            context.logger.log(Level.WARNING, "Exception during download of reports", ex)
            response = request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body("File not found")
                .header("Content-Type", "text/html")
                .build()
        }
        return response
    }

    fun GetReportById( request: HttpRequestMessage<String?>,  reportId: String, context: ExecutionContext ): HttpResponseMessage {
        val authClaims = checkAuthenticated(request, context)
        if( authClaims == null ) return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()
        var response: HttpResponseMessage

            response = request.createResponseBuilder(HttpStatus.NOT_IMPLEMENTED)
                .body("Not Implemented")
                .header("Content-Type", "text/html")
                .build()

                return response;
    }

    fun isToday( date: OffsetDateTime ) : Boolean {
        return date.monthValue == OffsetDateTime.now().monthValue &&
               date.dayOfMonth == OffsetDateTime.now().dayOfMonth &&
               date.year == OffsetDateTime.now().year;
    }

    fun isYesterday( date: OffsetDateTime ) : Boolean {
        var yesterday = OffsetDateTime.now().minusDays(1L);
        return date.monthValue == yesterday.monthValue &&
               date.dayOfMonth == yesterday.dayOfMonth &&
               date.year == yesterday.year;
    }

    fun GetSummaryTests(
        request: HttpRequestMessage<String?>,
        context: ExecutionContext
    ) : HttpResponseMessage {
        val authClaims = checkAuthenticated(request, context)
        if( authClaims == null ) return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()
        var response: HttpResponseMessage

        try{
            val headers = workflowEngine.db.fetchDownloadableReportFiles(
                OffsetDateTime.now().minusDays(DAYS_TO_SHOW), 
                authClaims.organization.name
            )
            var daily: Long = 0L
            var last : Long = 0L;
            var sum : Long = 0L;
            var data : Array<Long> = arrayOf(0,0,0,0,0,0,0,0)

            @Suppress( "NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER" )
            headers.sortedByDescending{ it.createdAt }.forEach {
                if( isToday( it.createdAt ) ) daily += it.itemCount.toLong();
                if( isYesterday( it.createdAt ) ) last += it.itemCount.toLong();                
                sum += it.itemCount.toLong()
                val expires: Int = (DAYS_TO_SHOW - it.createdAt.until(OffsetDateTime.now(), ChronoUnit.DAYS)).toInt();
                data.set(expires, data.get(expires) + it.itemCount.toLong()); 
            }

            val avg = if( headers.size >0 ) sum / headers.size else 0;

            var card = CardView.Builder()
                        .id( "summary-tests")
                        .title("Summay tests")
                        .subtitle("summary tests")
                        .daily(daily)
                        .last( last )
                        .positive( daily > avg )
                        .change( last - daily )
                        .pct_change( if (last > 0) ((last-daily)/last).toDouble() * 100 else 0.0 )
                        .data( data )
                        .build();
            response = request.createResponseBuilder(HttpStatus.OK)
                        .body( card )
                        .build();

        } 
        catch (ex: Exception) {
            context.logger.log(Level.WARNING, "Exception during download of summary/tests", ex)
            response = request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body("File not found")
                .header("Content-Type", "text/html")
                .build()
        }           
        return response;
    }

    fun GetSummary(request: HttpRequestMessage<String?>, 
                    field: String,
                   context: ExecutionContext): HttpResponseMessage {
        val authClaims = checkAuthenticated(request, context)
        if( authClaims == null ) return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()
        var response: HttpResponseMessage;
        try{
            val headers = workflowEngine.db.fetchDownloadableReportFiles(
                OffsetDateTime.now().minusDays(DAYS_TO_SHOW), authClaims.organization.name
            )

            @Suppress( "NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER" )
            var reports = headers.sortedByDescending{ it.createdAt }.map{
                if( it.bodyFormat == "CSV") getFieldSummaryForReportId(field,it.reportId.toString(), authClaims) else arrayListOf()
            }

            response = request.createResponseBuilder(HttpStatus.OK)
                .body( reports )
                .header("Content-Type", "application/json")
                .build()
        }catch (ex: Exception) {
            context.logger.log(Level.WARNING, "Exception during download of reports", ex)
            response = request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body("File not found")
                .header("Content-Type", "text/html")
                .build()
        }
        return response
    }

    fun getFieldSummaryForReportId( fieldName: String, reportId: String, authClaim: AuthClaims ): ArrayList<Facility> {
        var header: Header?
        var csv: FuzzyCSVTable? = null
        var facilties: ArrayList<Facility> = ArrayList<Facility>();

        try{
            header = workflowEngine.fetchHeader( ReportId.fromString(reportId), authClaim.organization )
        } catch (ex:Exception){ header = null }
        if( header !== null )
            csv = FuzzyCSVTable.parseCsv( StringReader( String(header.content!!) ) );
        if( csv !== null ){
            csv = csv.summarize( fieldName, count( fieldName ).az( "Count" ) )
            csv.forEach{
                facilties.add( Facility.Builder()
                                .facility( it.getAt(0).toString() )
                                .total( it.getAt(1).toString().toLong() )
                                .build()
                                )
            }
        }
        return facilties;
    }

    data class AuthClaims(
        val userName: String,
        val organization: Organization
    )

    /**
     * returns null if not authorized, otherwise returns a set of claims.
     */
    fun checkAuthenticated(request: HttpRequestMessage<String?>, context: ExecutionContext): AuthClaims? {
        var userName = ""
        var orgName = ""
        var jwtToken = request.headers["authorization"] ?: ""

        jwtToken = if( jwtToken.length > 7 ) jwtToken.substring(7) else "";

        if (jwtToken.isNotBlank()) {
            try {
                val jwtClaims = JSONObject(String(Base64.getDecoder().decode(jwtToken.split('.')[1])))
                userName = jwtClaims.getString("sub")
                val orgs = jwtClaims.getJSONArray("organization")
                @Suppress( "UNCHECKED_CAST")
                var org = if (orgs !== null) orgs.getString(0) else ""
                orgName = if (org.length > 3) org.substring(2) else ""
            } catch (ex: Throwable) {
                System.out.println(ex)
            }
        }
        if (userName.isNotBlank() && orgName.isNotBlank()) {
            val organization = WorkflowEngine().settings.findOrganization(orgName.replace('_', '-'))
            if (organization != null) {
                return AuthClaims(userName, organization)
            } else {
                context.logger.info("User $userName failed auth: Organization $orgName is unknown to the system.")
            }
        }
        return null
    }
}