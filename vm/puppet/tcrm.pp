include "epel"
include "vgl_common"
include "python_pip"
# include "python_venv"
include "puppi"
include "autofsck"

class tcrmdeps {
  # Required packages
  package { ["geos-devel", "git", "hdf5-devel"]:
    ensure => installed,
    require => [Class["epel"], Class["vgl_common"]]
  }

  class { python_venv: version => "2.7.8", }

  # Setup a virtualenv
  exec { "Set up a virtualenv to run in":
    command => "/usr/local/bin/virtualenv-2.7 /opt/tcrm_venv",
    creates => "/opt/tcrm_venv",
    alias => "tcrm_venv",
    require => [Class["python_venv"]],
  }

  # Install python dependencies

}

# # Explicitly allow externals so we can get basemap
# # TODO: Find out how to pass parameters to the 'pip' provider
# exec { "basemap_install":
#   command => "/usr/bin/pip install --allow-all-external --allow-unverified basemap basemap",
#   require => [Class["epel"], Class["python_pip"], Class["vgl_common"], Class["tcrmdeps"]],
#   timeout => 0,
# }

# # package { ["basemap"]:
# #   ensure => latest,
# #   provider => "pip",
# #   require => [Class["epel"], Class["python_pip"], Class["vgl_common"], Class["tcrmdeps"]],
# # }

# # We need the argparse module for python 2.6
# # TODO: Remove this if/when we migrate to 2.7+
# package { ["netCDF4", "argparse"]:
#   ensure => latest,
#   provider => "pip",
#   require => [Class["epel"], Class["python_pip"], Class["vgl_common"], Class["tcrmdeps"]],
# }

#Checkout, configure and install tcrm
exec { "tcrm-co":
  cwd => "/opt",
  command => "/usr/bin/git clone https://github.com/GeoscienceAustralia/tcrm.git",
  creates => "/opt/tcrm",
  require => [Class["tcrmdeps"]],
  timeout => 0,
}

class {"tcrmdeps": }

exec { "tcrm-setup":
  cwd => "/opt/tcrm",
  command => "/usr/local/bin/python installer/setup.py build_ext -i",
  require => [Exec["tcrm-co"]],
  timeout => 0,
}
