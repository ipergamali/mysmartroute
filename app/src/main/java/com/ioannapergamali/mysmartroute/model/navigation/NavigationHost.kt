package com.ioannapergamali.mysmartroute.model.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ioannapergamali.mysmartroute.view.ui.screens.HomeScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ResetPasswordScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SignUpScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.MenuScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RegisterVehicleScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.AnnounceTransportScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DeclareRouteScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DirectionsMapScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PoIListScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DefinePoiScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DefineDurationScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SettingsScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.AboutScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SupportScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ThemePickerScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.FontPickerScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SoundPickerScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DatabaseMenuScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.LocalDatabaseScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.FirebaseDatabaseScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.AdminSignUpScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.EditPrivilegesScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.DatabaseSyncScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RolesScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ProfileScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RouteEditorScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ManageFavoritesScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PrintCompletedScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PrintListScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PrintScheduledScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PrintDeclarationsScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PrintTicketScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PrepareCompleteRouteScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ViewVehiclesScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ViewUsersScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ViewTransportRequestsScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ViewRequestsScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.PassengerMovingsScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.WalkingScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.WalkingRoutesScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.BookSeatScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.FindVehicleScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.FindPassengersScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RouteModeScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ViewRoutesScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.SelectRoutePoisScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.AvailableTransportsScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.NotificationsScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.ReservationDetailsScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RankTransportsScreen
import com.ioannapergamali.mysmartroute.view.ui.screens.RankDriversScreen
import com.ioannapergamali.mysmartroute.R



@Composable
fun NavigationHost(
    navController: NavHostController,
    openDrawer: () -> Unit,
    startDestination: String = "home",
    requestId: String? = null
) {

    NavHost(navController = navController, startDestination = startDestination) {

        composable("home") {
            HomeScreen(
                navController = navController ,
                onNavigateToSignUp = {
                    navController.navigate("Signup")
                },
                openDrawer = openDrawer
            )
        }
        composable("resetPassword") {
            ResetPasswordScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("Signup") {
            SignUpScreen(
                navController = navController,
                onSignUpSuccess = {
                    navController.navigate("home") {
                        popUpTo("Signup") { inclusive = true }
                    }
                },
                openDrawer = openDrawer
            )
        }
        composable("menu") {
            MenuScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("registerVehicle") {
            RegisterVehicleScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("declareRoute") {
            DeclareRouteScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("announceAvailability") {
            AnnounceTransportScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("viewPois") {
            PoIListScreen(navController = navController, openDrawer = openDrawer)
        }
        composable("editRoute") {
            RouteEditorScreen(navController = navController, openDrawer = openDrawer)
        }
        composable(
            route = "definePoi?lat={lat}&lng={lng}&source={source}&view={view}&routeId={routeId}",
            arguments = listOf(
                navArgument("lat") { defaultValue = "" },
                navArgument("lng") { defaultValue = "" },
                navArgument("source") { defaultValue = "" },
                navArgument("view") { defaultValue = "false" },
                navArgument("routeId") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
            val source = backStackEntry.arguments?.getString("source")
            val viewOnly = backStackEntry.arguments?.getString("view")?.toBoolean() ?: false
            val routeIdArg = backStackEntry.arguments?.getString("routeId")
            DefinePoiScreen(
                navController = navController,
                openDrawer = openDrawer,
                initialLat = lat,
                initialLng = lng,
                source = source,
                viewOnly = viewOnly,
                routeId = routeIdArg
            )
        }
        composable("defineDuration") {
            DefineDurationScreen(navController = navController, openDrawer = openDrawer)
        }




        composable(
            route = "directionsMap/{start}/{end}",
            arguments = listOf(
                navArgument("start") { defaultValue = "" },
                navArgument("end") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val start = backStackEntry.arguments?.getString("start") ?: ""
            val end = backStackEntry.arguments?.getString("end") ?: ""
            DirectionsMapScreen(navController = navController, start = start, end = end, openDrawer = openDrawer)
        }

        composable("settings") {
            SettingsScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("themePicker") {
            ThemePickerScreen(navController = navController)
        }

        composable("fontPicker") {
            FontPickerScreen(navController = navController)
        }

        composable("roles") {
            RolesScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("profile") {
            ProfileScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("manageFavorites") {
            ManageFavoritesScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("routeMode") {
            BookSeatScreen(
                navController = navController,
                openDrawer = openDrawer,
                titleRes = R.string.route_mode,
                restrictToAvailableDates = false
            )
        }

        composable("findVehicle") {
            FindVehicleScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("findWay") {
            RouteModeScreen(
                navController = navController,
                openDrawer = openDrawer,
                titleRes = R.string.find_way,
                includeCost = true
            )
        }

        composable("findPassengers") {
            FindPassengersScreen(navController = navController, openDrawer = openDrawer)
        }

        composable(
            "availableTransports?routeId={routeId}&startId={startId}&endId={endId}&maxCost={maxCost}&date={date}",
            arguments = listOf(
                navArgument("routeId") { defaultValue = "" },
                navArgument("startId") { defaultValue = "" },
                navArgument("endId") { defaultValue = "" },
                navArgument("maxCost") { defaultValue = "" },
                navArgument("date") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val rid = backStackEntry.arguments?.getString("routeId")
            val sid = backStackEntry.arguments?.getString("startId")
            val eid = backStackEntry.arguments?.getString("endId")
            val maxCost = backStackEntry.arguments?.getString("maxCost")?.toDoubleOrNull()
            val date = backStackEntry.arguments?.getString("date")?.toLongOrNull()
            AvailableTransportsScreen(
                navController = navController,
                openDrawer = openDrawer,
                routeId = rid,
                startId = sid,
                endId = eid,
                maxCost = maxCost,
                date = date
            )
        }

        composable("bookSeat") {
            BookSeatScreen(
                navController = navController,
                openDrawer = openDrawer,
                titleRes = R.string.book_seat,
                restrictToAvailableDates = true
            )
        }

        composable("viewRoutes") {
            ViewRoutesScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("notifications") {
            NotificationsScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("selectRoutePois") {
            SelectRoutePoisScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("viewRequests") {
            ViewRequestsScreen(
                navController = navController,
                openDrawer = openDrawer,
                initialRequestId = requestId
            )
        }

        composable("viewMovings") {
            PassengerMovingsScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("walking") {
            WalkingScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("walkingRoutes") {
            WalkingRoutesScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("rankTransports") {
            RankTransportsScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("rankDrivers") {
            RankDriversScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("printTicket") {
            PrintTicketScreen(navController = navController, openDrawer = openDrawer)
        }

        composable(
            route = "reservationDetails/{reservation}",
            arguments = listOf(navArgument("reservation") { defaultValue = "" })
        ) { backStackEntry ->
            val reservationJson = backStackEntry.arguments?.getString("reservation")
            ReservationDetailsScreen(navController = navController, reservationJson = reservationJson)
        }

        composable("printList") {
            PrintListScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("printScheduled") {
            PrintScheduledScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("printCompleted") {
            PrintCompletedScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("printDeclarations") {
            PrintDeclarationsScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("prepareCompleteRoute") {
            PrepareCompleteRouteScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("viewTransportRequests") {
            ViewTransportRequestsScreen(
                navController = navController,
                openDrawer = openDrawer,
                initialRequestId = requestId
            )
        }

        composable("viewVehicles") {
            ViewVehiclesScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("viewUsers") {
            ViewUsersScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("soundPicker") {
            SoundPickerScreen(navController = navController)
        }

        composable("about") {
            AboutScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("support") {
            SupportScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("databaseMenu") {
            DatabaseMenuScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("localDb") {
            LocalDatabaseScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("firebaseDb") {
            FirebaseDatabaseScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("syncDb") {
            DatabaseSyncScreen(navController = navController, openDrawer = openDrawer)
        }

        composable("adminSignup") {
            AdminSignUpScreen(
                navController = navController,
                onSignUpSuccess = {
                    navController.popBackStack()
                },
                openDrawer = openDrawer
            )
        }

        composable("editPrivileges") {
            EditPrivilegesScreen(navController = navController, openDrawer = openDrawer)
        }






    }
}
