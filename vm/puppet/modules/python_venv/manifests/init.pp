# Downloads and installs a python version from source, plus pip/virtualenv

include "vgl_common"
include "puppi"

class python_venv( $version = '2.7.8' ) {
  # Download and install python
  puppi::netinstall { "Install python ${version}":
    url => "http://www.python.org/ftp/python/${version}/Python-${version}.tgz",
    destination_dir => "/usr/local/src",
    postextract_cwd => "/usr/local/src/Python-${version}",
    postextract_command => "/usr/local/src/Python-${version}/configure --prefix=/usr/local && /usr/bin/make && /usr/bin/make altinstall",
    alias => "inst_${version}",
    require => Class["vgl_common"],
  }

  # Install setuptools
  puppi::netinstall { "Install setuptools for python ${version}":
    url => "https://pypi.python.org/packages/source/s/setuptools/setuptools-5.4.1.tar.gz",
    extracted_dir => "setuptools-5.4.1",
    destination_dir => "/usr/local/src",
    postextract_cwd => "/usr/local/src/setuptools-5.4.1",
    postextract_command => "/usr/local/bin/python${version.sub(/(\d+(\.\d+)?).*/, '\1')} setup.py install",
    alias => "setup_${version}",
    require => Puppi::Netinstall["inst_${version}"],
  }

  # Install pip
  puppi::netinstall { "Install pip for python ${version}":
    url => "https://raw.github.com/pypa/pip/master/contrib/get-pip.py",
    destination_dir => "/usr/local/src",
    extract_command => "rsync",
    postextract_cwd => "/usr/local/src",
    postextract_command => "/usr/local/bin/python${version.sub(/(\d+(\.\d+)?).*/, '\1')} /usr/local/src/get-pip.py",
    creates => "/usr/local/bin/pip${version.sub(/(\d+(\.\d+)?).*/, '\1')}"
    alias => "pip_${version}",
    require => Puppi::Netinstall["setup_${version}"],
  }

  # Virtualenv
  exec { "Install virtualenv for python ${version}":
    command => "/usr/local/bin/pip${version.sub(/(\d+(\.\d+)?).*/, '\1')} install virtualenv",
    creates => "/usr/local/bin/virtualenv-${version.sub(/(\d+(\.\d+)?).*/, '\1')}",
    require => Puppi::Netinstall["pip_${version}"],
    timeout => 0,
  }
}
