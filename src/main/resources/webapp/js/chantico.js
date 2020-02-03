Chantico = (function () {
	console.log('Chantico UI Module loaded');
	var module = {};

	/**
	 * Opens a toast message with a specific type and content
	 * @param messageType String with the message type: info, success, warning, error
	 * @param messageText Text to display inside the toast
	 * @param closeCallback Optional function to be invoked after the toast has been dismissed
	 */
	module.toast = function(messageType, messageText, closeCallback) {
		var toastContainer = $('#divToastContainer');
		if (toastContainer.length === 0) {
			toastContainer = $('<div id="divToastContainer" class="toast-container"></div>');
			toastContainer.appendTo(document.body);
		}
		var toastDiv = $('<div class="toast notification">' + messageText + '</div>');
		if (messageType === 'info') {
			toastDiv.addClass('is-info');
		} else if (messageType === 'success') {
			toastDiv.addClass('is-success');
		} else if (messageType === 'warning') {
			toastDiv.addClass('is-warning');
		} else if (messageType === 'error') {
			toastDiv.addClass('is-danger');
		}
		toastDiv.appendTo(toastContainer);
		toastDiv.fadeIn(function() {
			setTimeout(function() {
				toastDiv.fadeOut(function() {
					toastDiv.remove();
					if (typeof(closeCallback) === 'function') {
						closeCallback();
					}
				});
			}, 5000);
		});
	};

	/** Clear all the toasts being displayed in the UI */
	module.toast.clear = function() {
		$('#divToastContainer').html('');
	};

	/**
     * Executes a form submit using AJAX
     * @param formSelector String with the selector to identify the form
     * @param preExecution Optional function to execute before submitting the data it can prevent the execution by using a boolean return value
	 * @returns A promise to be fullfilled or rejected depending on the resonse received from the server side
     */
	module.onFormSubmit = function (formSelector, preExecution) {
		return new RSVP.Promise(function(resolve, reject) {
			$(formSelector).on('submit', function (evt) {
				evt.preventDefault();
				if (typeof (preExecution) !== 'function' || preExecution() === true) {
					$.ajax({
						type: $(this).attr('method') || 'POST',
						cache: false,
						url: $(this).attr('action'),
						data: $(this).serialize(),
						success: resolve,
						error: function (xhr, status, error) {
							if (xhr && xhr.responseText) {
								reject(xhr.responseText);
							} else {
								reject();
							}
						}
					});
				}
			});
		});
	};

	/** Generates the menu for the current user */
	module.menu = function () {
		return new RSVP.Promise(function (resolve, reject) {
			var menuNav = $('<nav id="chanticoMenu" class="main-menu"></nav>');
			var itemContainer = $('<ul></ul>');
			// FIXME: read menu from service
			var menuSpecs = [{ icon: 'home', label: 'Home', link: '/' }, { icon: 'key', label: 'Security', link: '/' }, { icon: 'cog', label: 'Settings', link: '/' }];
			for (var idx = 0; idx < menuSpecs.length; idx++) {
				var spec = menuSpecs[idx];
				$('<li><a href="' + spec.link + '"><i class="las la-' + spec.icon + '"></i><span class="nav-text">' + spec.label + '</span></a></li>').appendTo(itemContainer);
			}
			itemContainer.appendTo(menuNav);

			itemContainer = $('<ul class="bottom"></ul>');
			$('<li><a href=""><i class="las la-power-off"></i><span class="nav-text">Logout</span></a></li>').appendTo(itemContainer);
			itemContainer.appendTo(menuNav);

			menuNav.appendTo(document.body);
			resolve();
		});
	};

	/** Creates a DOM element representing a label and a value */
	module.labeledValue = function(label, value) {
		return $('<div class="labeled-value"><div class= "label">' + label + ':</div><div class="value">' + value + '</div></div>');
	};

	/** Trnasform a boolean flag into a toggle icon */
	module.asToggle = function(value) {
		if(value === true) {
			return '<i class="las la-toggle-on"></i>';
		} else {
			return '<i class="las la-toggle-off"></i>';
		}
	};

	/** Extracts the list of existing repositories */
	module.getRepositories = function () {
		return new RSVP.Promise(function (resolve, reject) {
			// FIXME: invoke the server service
			resolve([
				{ id: 745647546845, name: 'master', artifacts: 34, proxy: false, cache: true },
				{ id: 754657467455, name: 'alux', artifacts: 34, proxy: true, cache: false },
				{ id: 463546756753, name: 'snapshots', artifacts: 34, proxy: true, cache: true },
				{ id: 564587878965, name: 'releases', artifacts: 34, proxy: false, cache: false }
			]);
		});
	};

	/** Retrieves the information for a single repository */
	module.getRepository = function() {
		return new RSVP.Promise(function (resolve, reject) {
			resolve({
				repo: { name: 'master', artifacts: 34, proxy: false, cache: true },
				artifacts: [
					{ id: 4564678979, group: 'org.delightful', artifact: 'harpo', latest: '2.1.0', versions: 34, cached: false },
					{ id: 7897645646, group: 'org.spring', artifact: 'core', latest: '1.0', versions: 2, cached: true }
				],
				activity: [
					{ repo: 'master', group: 'org.codelightful', artifact: 'harpo', version: '2.0.1', user: 'malcomx', date: 1580029320320 },
					{ repo: 'master', group: 'org.codelightful', artifact: 'harpo', version: '2.0.2', user: 'hoboken', date: 1581029331200 },
					{ repo: 'master', group: 'org.codelightful', artifact: 'harpo', version: '2.1', user: 'johndoe', date: 1581149671253 }
				]
			});
		});
	}

	/** Retrieves the information for a single artifact */
	module.getArtifact = function () {
		return new RSVP.Promise(function (resolve, reject) {
			resolve({
				artifact: { id: 4564678979, group: 'org.delightful', artifact: 'harpo', latest: '2.1.0', versions: 34, cached: false },
				versions: [
					{ label: '1.2.2', release: 1581149671253, user: 'johndoe' },
					{ label: '1.2.1', release: 1581139671253, user: 'johndoe' },
					{ label: '1.2.0', release: 1581109671253, user: 'johndoe' },
					{ label: '1.1.0', release: 1581099671253, user: 'johndoe' },
					{ label: '1.0.0', release: 1581089671253, user: 'johndoe' }
				]
			});
		});
	}

	/** Extracts the list of existing activities */
	module.getRecentActivity = function (containerId, repoId) {
		var promise = new RSVP.Promise(function (resolve, reject) {
			// FIXME: invoke the server service
			resolve([
				{ repo: 'master', group: 'org.codelightful', artifact: 'serpent', version: '3.0.4', user: 'johndoe', date: 1573529320320 },
				{ repo: 'relases', group: 'org.sprint', artifact: 'fabulous', version: '1.0-SNAPSHOT', user: 'amartino', date: 1574029320320 },
				{ repo: 'alux', group: 'org.something', artifact: 'allure', version: '1.0.4', user: 'janedoe', date: 1578029320320 },
				{ repo: 'master', group: 'org.codelightful', artifact: 'harpo', version: '2.0.1', user: 'malcomx', date: 1580029320320 },
				{ repo: 'master', group: 'org.codelightful', artifact: 'harpo', version: '2.0.2', user: 'hoboken', date: 1581029331200 },
				{ repo: 'master', group: 'org.codelightful', artifact: 'harpo', version: '2.1', user: 'johndoe', date: 1581149671253 },
			]);
		});
		promise.then(function(response) {
			renderRecentList(containerId, response);
		});
	};

	/** Render a list of recent activity inside a specific container */
	module.renderRecentList = function(containerId, data) {
		var container = $(containerId);
		if(!data || data.length == 0) {
			container.html('<div class="notification">No recent activity</div>');
			return;
		}
		container.html('');
		for (var idx = 0; idx < data.length; idx++) {
			container.append(createRecentRow(data[idx]));
		}
	};

	/** Internal method to create a row for recent activity */
	function createRecentRow(entry) {
		var row = $('<div class="notification artifact"></div>');
		row.append('<div><b>Repository:</b> ' + entry.repo + '</div>');
		row.append('<div><b>Date:</b> ' + new Date(entry.date) + '</div>');
		row.append('<div><b>Author:</b> ' + entry.user + '</div>');
		var artifact = $('<div class="tags has-addons"><span class="tag is-primary">Artifact</span></div>');
		artifact.append('<span class="tag is-dark">' + entry.group + '</span>');
		artifact.append('<span class="tag is-black">' + entry.artifact + '</span>');
		artifact.append('<span class="tag is-dark">' + entry.version + '</span>');
		artifact.appendTo(row);
		return row;
	}

	return module;
})();